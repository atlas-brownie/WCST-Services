package gov.va.benefits.service.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ValidationException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gov.va.benefits.domain.ClaimRecord;
import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;
import gov.va.benefits.service.CSPInterfaceService;
import gov.va.benefits.service.ClaimsService;
import gov.va.benefits.utils.HttpClientBean;

/**
 * Claim Service implementation responsible for validating the claim processing
 * requests, uploading valid requests and monitoring of claims processing...
 * 
 * @author L Antony
 *
 */
@Service
public class ClaimServiceImpl implements ClaimsService {
	private static Logger LOGGER = LoggerFactory.getLogger(ClaimServiceImpl.class);

	@Value("${clientSystemId:MBL-WCST}")
	private String clientSystemId;

	@Value("${sourceDocumentType:21-22}")
	private String sourceDocumentType;

	@Value("${vaAuthHeaderKey:apikey}")
	private String vaAuthHeaderKey;

	@Value("${vaAuthHeaderValue}")
	private String vaAuthHeaderValue;

	@Value("${vaClaimIntakePointerUrl:https://sandbox-api.va.gov/services/vba_documents/v1/uploads}")
	private String claimsIntakePointerUrl;

	@Value("${performETagValidation:false}")
	private boolean performETagValidation;

	@Autowired
	private CSPInterfaceService cspService;

	@Autowired
	private HttpClientBean httpClientBean;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.va.benefits.service.ClaimsService#processClaimRequest(gov.va.benefits.dto
	 * .ClaimDetails)
	 */
	@Override
	public ClaimStatusResponse processClaimRequest(ClaimDetails aClaimDetails) throws IOException {
		LOGGER.debug("begin processClaimRequest()...");

		validateClaimRequest(aClaimDetails);

		ClaimRecord claimRecord = submitClaimRequest(aClaimDetails);

		ClaimStatusResponse statusResponse = saveClaimDetails(claimRecord);

		LOGGER.debug("end processClaimRequest()...");

		return statusResponse;
	}

	/**
	 * Responsible for performing semantic validations on request attributes...
	 * 
	 * @param aClaimDetails
	 * @throws ValidationException
	 */
	private void validateClaimRequest(ClaimDetails aClaimDetails) throws ValidationException {
		if (aClaimDetails.getClaimFileName() == null) {
			throw new ValidationException("Missing Claim File Info!");
		}

		if (aClaimDetails.getClaimeFileContent() == null || aClaimDetails.getClaimeFileContent().length <= 0) {
			throw new ValidationException("Claim File Cannot be Empty!");
		}

		if (StringUtils.isEmpty(aClaimDetails.getSsn())) {
			throw new ValidationException("SSN Cannot be Empty!");
		}
	}

	/**
	 * Saves claim submission related meta-data in the database and returns status
	 * response object...
	 * 
	 * @param aClaimRecord
	 * @return
	 */
	private ClaimStatusResponse saveClaimDetails(ClaimRecord aClaimRecord) {
		cspService.saveClaimDetails(aClaimRecord);

		ClaimStatusResponse statusResponse = new ClaimStatusResponse();
		statusResponse.setFirstName(aClaimRecord.getFirstName());
		statusResponse.setLastName(aClaimRecord.getLastName());
		statusResponse.setClaimStatus(aClaimRecord.getCurrentStatus());
		statusResponse.setTrackingCode(aClaimRecord.getSimpleTrackingCode());
		statusResponse.setVaTrackingCode(aClaimRecord.getVaTrackerCode());
		statusResponse.setSubmissionDate(aClaimRecord.getSubmissionDate());

		return statusResponse;
	}

	/**
	 * Takes in claims details and uploads the claim file and other meta-data
	 * information to the VA benefit application...
	 * 
	 * @param aClaimDetails
	 * @return
	 * @throws IOException
	 */
	private ClaimRecord submitClaimRequest(ClaimDetails aClaimDetails) throws IOException {
		Pair<String, String> endpointInfo = extractIntakeEndpointInfo();

		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		String metaDataStr = String.format(
				"{\"veteranFirstName\": \"%s\",\"veteranLastName\": \"%s\",\"fileNumber\": \"%s\",\"zipCode\": \"%s\",\"source\": \"%s\",\"docType\": \"%s\"}",
				aClaimDetails.getFirstName(), aClaimDetails.getLastName(),
				StringUtils.leftPad(StringUtils.substring(aClaimDetails.getUnformattedSSN(), 0, 8), 8, "0"),
				aClaimDetails.getZipCode(), sourceDocumentType, sourceDocumentType);

		entityBuilder.addBinaryBody("metadata", metaDataStr.getBytes(), ContentType.APPLICATION_JSON, "metadata.json");
		entityBuilder.addBinaryBody("content", aClaimDetails.getClaimeFileContent(),
				ContentType.create("application/pdf"), aClaimDetails.getClaimFileName());

		HttpEntity mutiPartHttpEntity = entityBuilder.build();
		RequestBuilder reqBuilder = RequestBuilder.put(endpointInfo.getLeft());
		reqBuilder.setEntity(mutiPartHttpEntity);

		HttpUriRequest multipartRequest = reqBuilder.build();

		ResponseHandler<String> responseHandler = response -> {
			int status = response.getStatusLine().getStatusCode();
			if (status >= 200 && status < 300) {
				Header[] eTagHeaders = response.getHeaders("ETag");
				if (eTagHeaders != null && eTagHeaders.length >= 0) {
					return eTagHeaders[0].getValue();
				}

				return null;
			} else if (status == 403) {
				throw new ClientProtocolException("Response status: " + status + " Unauthorized");
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}
		};

		String eTagHeaderValue = httpClientBean.execute(multipartRequest, responseHandler);

		ClaimRecord claimRec = populateClaimRecord(aClaimDetails, endpointInfo);

		byte[] payloadBytes = extractPayloadSent(mutiPartHttpEntity);

		validateResponse(claimRec, eTagHeaderValue, payloadBytes);

		return claimRec;
	}

	private byte[] extractPayloadSent(HttpEntity aMutiPartHttpEntity) {
		if (aMutiPartHttpEntity.isRepeatable()) {
			try {
				return IOUtils.toByteArray(aMutiPartHttpEntity.getContent());
			} catch (UnsupportedOperationException | IOException exp) {
				LOGGER.info("Failed to read stream content!", exp);
			}
		}

		return null;
	}

	/**
	 * Perform post validation of response received from VA application...
	 * 
	 * @param claimRec
	 * @param eTagHeaderValue
	 * @param payload
	 * @throws IOException
	 */
	private void validateResponse(ClaimRecord claimRec, String eTagHeaderValue, byte[] payload) throws IOException {
		String requestStatus = extractRequestStatusByVaTrackingNumber(claimRec.getVaTrackerCode());

		claimRec.setCurrentStatus(requestStatus);

		if (!performETagValidation || StringUtils.isBlank(eTagHeaderValue) || payload == null) {
			return;
		}

		// if MD5 hashes don't match, throw an exception here...
		MessageDigest md5Instance = null;
		try {
			md5Instance = MessageDigest.getInstance("MD5");

		} catch (NoSuchAlgorithmException exp) {
			throw new IOException("Unable to initialize MD5 Digest!", exp);
		}

		md5Instance.update(payload);
		byte[] generatedDigest = md5Instance.digest();

		byte[] receivedDigest;
		try {
			String eTagHeaderHeader = eTagHeaderValue.replaceAll("\"", "");
			receivedDigest = Hex.decodeHex(eTagHeaderHeader);
		} catch (DecoderException exp) {
			throw new IOException("Unable to decode received eTa Header!", exp);
		}

		if (!MessageDigest.isEqual(generatedDigest, receivedDigest)) {
			throw new IOException("Failed to Validate Message Digests!");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.va.benefits.service.ClaimsService#
	 * extractRequestStatusBySimpleTrackingCode(java.lang.String)
	 */
	@Override
	public String extractRequestStatusBySimpleTrackingCode(String simpleTrackingCode)
			throws IOException, ClientProtocolException {
		Map<String, String> searchCriteria = new HashMap<>();

		searchCriteria.put("simpleTrackingCode", simpleTrackingCode);

		Set<ClaimRecord> claimRecords = cspService.findClaimRecord(searchCriteria);
		if (claimRecords == null || claimRecords.isEmpty()) {
			throw new IOException("Unable locate claim record!");
		}

		ClaimRecord claimRecord = claimRecords.stream().findFirst().get();

		return extractRequestStatusByVaTrackingNumber(claimRecord.getVaTrackerCode());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.va.benefits.service.ClaimsService#extractRequestStatusByVaTrackingNumber(
	 * java.lang.String)
	 */
	@Override
	public String extractRequestStatusByVaTrackingNumber(String vaTrackingNumber)
			throws IOException, ClientProtocolException {
		HttpGet extractClient = new HttpGet(String.format("%s/%s", claimsIntakePointerUrl, vaTrackingNumber));

		extractClient.setHeader(vaAuthHeaderKey, vaAuthHeaderValue);
		extractClient.addHeader("accept", "application/json");

		ResponseHandler<String> responseHandler = response -> {
			int status = response.getStatusLine().getStatusCode();
			if (status >= 200 && status < 300) {
				HttpEntity entity = response.getEntity();
				return entity != null ? EntityUtils.toString(entity) : null;
			} else if (status == 403) {
				throw new ClientProtocolException("Response status: " + status + " Unauthorized");
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}
		};

		String result = httpClientBean.execute(extractClient, responseHandler);

		JSONObject jsonObj = new JSONObject(result);

		String requestStatus = jsonObj.getJSONObject("data").getJSONObject("attributes").getString("status");

		return requestStatus;
	}

	/**
	 * Generates claim record that needs to be persisted in the database...
	 * 
	 * @param aClaimDetails
	 * @param aEndpointInfo
	 * @return
	 * @throws IOException
	 */
	private ClaimRecord populateClaimRecord(ClaimDetails aClaimDetails, Pair<String, String> aEndpointInfo)
			throws IOException {
		ClaimRecord claimRecord = new ClaimRecord();

		claimRecord.setSubmissionDate(new Date());
		claimRecord.setFirstName(aClaimDetails.getFirstName());
		claimRecord.setLastName(aClaimDetails.getLastName());
		claimRecord.setSsn(aClaimDetails.getSsn());
		claimRecord.setZipCode(aClaimDetails.getZipCode());
		claimRecord.setNumberRetries(0);

		claimRecord.setVaFileLocation(aEndpointInfo.getLeft());
		claimRecord.setVaTrackerCode(aEndpointInfo.getRight());

		claimRecord.setCurrentStatus("Pending");

		claimRecord.setClaimFileName(aClaimDetails.getClaimFileName());
		claimRecord.setClaimFileContent(aClaimDetails.getClaimeFileContent());

		return claimRecord;
	}

	/**
	 * Hits VA Benefits' REST-End point with an HTTP Post request and retrieves the
	 * target end-point URL to put claim file info and tracking number details for
	 * the same. The method returns a Pair and the first element of the pair will be
	 * the target end-point URL string and the second element will be the tracking
	 * number..
	 * 
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private Pair<String, String> extractIntakeEndpointInfo() throws ClientProtocolException, IOException {
		HttpPost extractClient = new HttpPost(claimsIntakePointerUrl);

		extractClient.setHeader(vaAuthHeaderKey, vaAuthHeaderValue);
		extractClient.addHeader("accept", "application/json");

		ResponseHandler<String> responseHandler = response -> {
			int status = response.getStatusLine().getStatusCode();
			if (status >= 200 && status < 300) {
				HttpEntity entity = response.getEntity();
				return entity != null ? EntityUtils.toString(entity) : null;
			} else if (status == 403) {
				throw new ClientProtocolException("Response status: " + status + " Unauthorized");
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}
		};

		String result = httpClientBean.execute(extractClient, responseHandler);

		JSONObject jsonObj = new JSONObject(result);

		String uploadLocation = jsonObj.getJSONObject("data").getJSONObject("attributes").getString("location");
		String guid = jsonObj.getJSONObject("data").getJSONObject("attributes").getString("guid");

		return new ImmutablePair<String, String>(uploadLocation, guid);
	}
}
