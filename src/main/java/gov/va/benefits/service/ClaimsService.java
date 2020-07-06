package gov.va.benefits.service;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;

/**
 * Service responsible for uploading and monitoring of VA claims processing...
 * 
 * @author L Antony
 *
 */
public interface ClaimsService {

	/**
	 * Takes in claims request data and uploads the same to VA benefit processing
	 * system and persist the request data for monitoring purposes...
	 * 
	 * @param aClaimDetails
	 * @return
	 * @throws IOException
	 */
	ClaimStatusResponse processClaimRequest(ClaimDetails aClaimDetails) throws IOException;

	/**
	 * Method that sends request to VA Benefits system and extracts the current
	 * status of the claim based on specifiedVA tracking code...
	 * 
	 * @param vaTrackingNumber
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	String extractRequestStatusByVaTrackingNumber(String vaTrackingNumber) throws IOException, ClientProtocolException;

	/**
	 * Method that sends request to VA Benefits system and extracts the current
	 * status of the claim based on specified simple tracking code...
	 * 
	 * @param simpleTrackingCode
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	String extractRequestStatusBySimpleTrackingCode(String simpleTrackingCode)
			throws IOException, ClientProtocolException;

}
