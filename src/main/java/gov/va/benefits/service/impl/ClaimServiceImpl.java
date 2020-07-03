package gov.va.benefits.service.impl;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;

import javax.validation.ValidationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gov.va.benefits.domain.ClaimRecord;
import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;
import gov.va.benefits.service.ClaimsService;

/**
 * 
 * @author Laljith Antony
 * 
 *         Claim Service implementation responsible for validating the claim
 *         processing requests, uploading valid requests and monitoring of VA
 *         claims processing...
 *
 */
@Service
public class ClaimServiceImpl implements ClaimsService {

	private static final String WEB_KIT_FORM_BOUNDARY = "------WebKitFormBoundaryVfOwzCyvug0JmWYo";
	private static final String CR_LF = "\r\n";

	private static final Object SOURCE = "MyVSO";
	private static final Object DOC_TYPE = "21-22";

	@Value("${vaAuthHeaderKey:apikey}")
	private String vaAuthHeaderKey;

	@Value("${vaAuthHeaderValue:RrJGQZc6jpNp9NwKEn6GO2ZDq11MbdXm}")
//	@Value("${vaAauthHeaderValue}")
	private String vaAuthHeaderValue;

	@Value("${vaClaimIntakePointerUrl:https://sandbox-api.va.gov/services/vba_documents/v1/uploads}")
	private String claimsIntakePointerUrl;

	@Override
	public ClaimStatusResponse processClaimRequest(ClaimDetails aClaimDetails) throws IOException {
		validateClaimRequest(aClaimDetails);

		ClaimRecord claimRecord = submitClaimRequest(aClaimDetails);

		ClaimStatusResponse statusResponse = saveClaimDetails(claimRecord);

		return statusResponse;
	}

	private void validateClaimRequest(ClaimDetails aClaimDetails) throws ValidationException {
		if (aClaimDetails.getClaimFile() == null) {
			throw new ValidationException("Missing Claim File Info!");
		}

		if (StringUtils.isBlank(aClaimDetails.getClaimFile().getName())) {
			throw new ValidationException("Claim File Name not Specified!");
		}

		if (aClaimDetails.getClaimFile().getSize() <= 0) {
			throw new ValidationException("Claim File Cannot be Empty!");
		}
	}

	private ClaimStatusResponse saveClaimDetails(ClaimRecord aClaimRecord) {
		ClaimStatusResponse statusResponse = new ClaimStatusResponse();
		statusResponse.setFirstName(aClaimRecord.getFirstName());
		statusResponse.setLastName(aClaimRecord.getLastName());
		statusResponse.setClaimStatus(aClaimRecord.getCurrentStatus());
		statusResponse.setTrackingNumber(aClaimRecord.getSimpleTrackingNumber());
		statusResponse.setSubmissionDate(aClaimRecord.getSubmissionDate());

		return statusResponse;
	}

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
				HttpEntity entity = response.getEntity();
				return entity != null ? EntityUtils.toString(entity) : null;
			} else if (status == 403) {
				throw new ClientProtocolException("Response status: " + status + " Unauthorized");
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}
		};

		httpclient.execute(httpPut, responseHandler);

		ClaimRecord claimRec = populateClaimRecord(aClaimDetails, endpointInfo);

		return claimRec;
	}

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
		claimRecord.setVaTrackerId(aEndpointInfo.getRight());

		claimRecord.setCurrentStatus("Pending");

		claimRecord.setClaimFileName(aClaimDetails.getClaimFile().getName());
		claimRecord.setClaimFileContent(aClaimDetails.getClaimFile().getBytes());

		// This needs to be fixed...
		String trackingNumber = generateSimpleTrackingNumber(claimRecord, aEndpointInfo);
		claimRecord.setSimpleTrackingNumber(trackingNumber);

		return claimRecord;
	}

	/**
	 * Fix this logic...
	 * 
	 * @param aClaimRecord
	 * @param aEndpointInfo
	 * @return
	 */
	private String generateSimpleTrackingNumber(ClaimRecord aClaimRecord, Pair<String, String> aEndpointInfo) {
//		String trackingNumber = String.format("%s%s-%04s-%04s",
//				StringUtils.substring(aClaimRecord.getFirstName(), 0, 1),
//				StringUtils.substring(aClaimRecord.getLastName(), 0, 1), StringUtils.substring(aClaimRecord.getSsn(),
//						StringUtils.length(aClaimRecord.getSsn()) - 4, StringUtils.length(aClaimRecord.getSsn())));
//
//		return trackingNumber;

		return aEndpointInfo.getRight();
	}

	private String generateClaimPayload(ClaimDetails aClaimsDetails) throws IOException {
		byte[] data = aClaimsDetails.getClaimFile().getBytes();

		String binaryData = new String(data);

		String metaDataStr = String.format(
				"{\"veteranFirstName\": \"%s\",\"veteranLastName\": \"%s\",\"fileNumber\": \"%s\",\"zipCode\": \"%s\",\"source\": \"%s\",\"docType\": \"%s\"}",
				aClaimsDetails.getFirstName(), aClaimsDetails.getLastName(), aClaimsDetails.getClaimFile().getName(),
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
				aClaimsDetails.getClaimFile().getName()));
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
