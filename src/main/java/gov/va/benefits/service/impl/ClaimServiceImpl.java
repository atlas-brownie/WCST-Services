package gov.va.benefits.service.impl;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;

import javax.validation.ValidationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gov.va.benefits.domain.ClaimRecord;
import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;
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

	private static final Object SOURCE = "MyVSO";
	private static final Object DOC_TYPE = "21-22";

	@Value("${vaAuthHeaderKey:apikey}")
	private String vaAuthHeaderKey;

	@Value("${vaAuthHeaderValue}")
	private String vaAuthHeaderValue;

	@Value("${vaClaimIntakePointerUrl:https://sandbox-api.va.gov/services/vba_documents/v1/uploads}")
	private String claimsIntakePointerUrl;

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

		validateResponse(eTagHeaderValue, payload);

		ClaimRecord claimRec = populateClaimRecord(aClaimDetails, endpointInfo);

		return claimRec;
	}

	/**
	 * Perform post validation of response received from VA application...
	 * 
	 * @param eTagHeaderValue
	 * @param payload
	 * @throws IOException
	 */
	private void validateResponse(String eTagHeaderValue, String payload) throws IOException {
		if (eTagHeaderValue == null) {
			return;
		}

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

		String trackingNumber = generateSimpleTrackingCode(claimRecord);
		claimRecord.setSimpleTrackingCode(trackingNumber);

		return claimRecord;
	}

	/**
	 * Generates user-friendly tracking code...
	 * 
	 * Please note that this method doesn't take into consideration multiple
	 * simultaneous instances of cluster nodes and no instance id related attributes
	 * are taken into consideration in generating tracking code for simplicity.
	 * Therefore in rare instance there could be collisions in generated tracking
	 * codes...
	 * 
	 * @param aClaimRecord
	 * @return
	 */
	private String generateSimpleTrackingCode(ClaimRecord aClaimRecord) {
		long currentTimeSec = System.currentTimeMillis() / 1000;

		String trackingNumber = StringUtils
				.upperCase(String.format("%s%s-%4s-%4s-%4s", StringUtils.substring(aClaimRecord.getFirstName(), 0, 1),
						StringUtils.substring(aClaimRecord.getLastName(), 0, 1),
						StringUtils.substring(aClaimRecord.getSsn(), StringUtils.length(aClaimRecord.getSsn()) - 4,
								StringUtils.length(aClaimRecord.getSsn())),
						StringUtils.leftPad(Long.toHexString(currentTimeSec / 10000), 4, '0'),
						StringUtils.leftPad(Long.toHexString(currentTimeSec % 10000), 4, '0')));

		System.out.println("Tracking Code:" + trackingNumber);

		return trackingNumber;
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
