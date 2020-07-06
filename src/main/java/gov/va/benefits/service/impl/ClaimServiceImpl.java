package gov.va.benefits.service.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ValidationException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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

	private static final String WEB_KIT_FORM_BOUNDARY = "------WebKitFormBoundaryVfOwzCyvug0JmWYo";
	private static final String CR_LF = "\r\n";

	private static final Object SOURCE = "MBL-WCST";
	private static final Object DOC_TYPE = "21-22";

	@Value("${vaAuthHeaderKey:apikey}")
	private String vaAuthHeaderKey;

	@Value("${vaAuthHeaderValue}")
	private String vaAuthHeaderValue;

	@Value("${vaClaimIntakePointerUrl:https://sandbox-api.va.gov/services/vba_documents/v1/uploads}")
	private String claimsIntakePointerUrl;

	@Value("${vaAuthHeaderValue:false}")
	private boolean performETagValidation;

	@Autowired
	private CSPInterfaceService cspService;

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

		// Optional Code to validate PDF content...
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
		Pair<String, String> endpointInfo = extractIntakeEndpoint();

		String payload = generateClaimPayload(aClaimDetails);

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPut httpPut = new HttpPut(endpointInfo.getLeft());
		httpPut.setHeader("Content-Type", "multipart/form-data");
		httpPut.setHeader(vaAuthHeaderKey, vaAuthHeaderValue);
		StringEntity stringEntity = new StringEntity(payload);
		httpPut.setEntity(stringEntity);

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

		String eTagHeaderValue = httpclient.execute(httpPut, responseHandler);

		ClaimRecord claimRec = populateClaimRecord(aClaimDetails, endpointInfo);

		validateResponse(claimRec, eTagHeaderValue, payload);

		return claimRec;
	}

	/**
	 * Perform post validation of response received from VA application...
	 * 
	 * @param claimRec
	 * @param eTagHeaderValue
	 * @param payload
	 * @throws IOException
	 */
	private void validateResponse(ClaimRecord claimRec, String eTagHeaderValue, String payload) throws IOException {
		String requestStatus = extractRequestStatusByVaTrackingNumber(claimRec.getVaTrackerCode());

		claimRec.setCurrentStatus(requestStatus);

		if (!performETagValidation || StringUtils.isBlank(eTagHeaderValue)) {
			return;
		}

		// if MD5 hashes don't match, throw an exception here...
		MessageDigest md5Instance = null;
		try {
			md5Instance = MessageDigest.getInstance("MD5");

		} catch (NoSuchAlgorithmException exp) {
			throw new IOException("Unable to initialize MD5 Digest!", exp);
		}

		md5Instance.update(payload.getBytes());
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
		CloseableHttpClient httpclient = HttpClients.createDefault();

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

		String result = httpclient.execute(extractClient, responseHandler);

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
	 * Generate 'stringified' representation of multipart request body that needs to
	 * be sent to VA benefits application...
	 * 
	 * @param aClaimsDetails
	 * @return
	 * @throws IOException
	 */
	private String generateClaimPayload(ClaimDetails aClaimsDetails) throws IOException {
		byte[] data = aClaimsDetails.getClaimeFileContent();

		String binaryData = new String(data);

		String metaDataStr = String.format(
				"{\"veteranFirstName\": \"%s\",\"veteranLastName\": \"%s\",\"fileNumber\": \"%s\",\"zipCode\": \"%s\",\"source\": \"%s\",\"docType\": \"%s\"}",
				aClaimsDetails.getFirstName(), aClaimsDetails.getLastName(), aClaimsDetails.getClaimFileName(),
				aClaimsDetails.getZipCode(), SOURCE, DOC_TYPE);

		StringBuffer payloadBuff = new StringBuffer();

		payloadBuff.append(WEB_KIT_FORM_BOUNDARY);
		payloadBuff.append(CR_LF);
		payloadBuff.append("Content-Disposition: form-data; name='metadata'; filename='metadata.json'");
		payloadBuff.append(CR_LF);
		payloadBuff.append("Content-Type: application/json");
		payloadBuff.append(CR_LF);
		payloadBuff.append(CR_LF);
		payloadBuff.append("");
		payloadBuff.append(CR_LF);
		payloadBuff.append(metaDataStr);
		payloadBuff.append(CR_LF);
		payloadBuff.append(WEB_KIT_FORM_BOUNDARY);
		payloadBuff.append(CR_LF);
		payloadBuff.append(String.format("Content-Disposition: form-data; name='content'; filename='%s'",
				aClaimsDetails.getClaimFileName()));
		payloadBuff.append(CR_LF);
		payloadBuff.append("Content-Type: application/pdf");
		payloadBuff.append(CR_LF);
		payloadBuff.append("");
		payloadBuff.append(CR_LF);
		payloadBuff.append(binaryData);
		payloadBuff.append(CR_LF);
		payloadBuff.append(WEB_KIT_FORM_BOUNDARY);

		String encodedPayload = Base64.getEncoder().encodeToString(payloadBuff.toString().getBytes());

		return "data:multipart/form-data;base64,".concat(encodedPayload);
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
	private Pair<String, String> extractIntakeEndpoint() throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();

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

		String result = httpclient.execute(extractClient, responseHandler);

		JSONObject jsonObj = new JSONObject(result);

		String uploadLocation = jsonObj.getJSONObject("data").getJSONObject("attributes").getString("location");
		String guid = jsonObj.getJSONObject("data").getJSONObject("attributes").getString("guid");

		return new ImmutablePair<String, String>(uploadLocation, guid);
	}
}
