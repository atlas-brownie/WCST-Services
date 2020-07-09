package gov.va.benefits.service;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.ClientProtocolException;

import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;
import gov.va.benefits.dto.DataExchangeJounalEntry;

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
	Pair<String, List<DataExchangeJounalEntry>> extractRequestStatusByVaTrackingNumber(String vaTrackingNumber)
			throws IOException, ClientProtocolException;

	/**
	 * Method that sends request to VA Benefits system and extracts the current
	 * status of the claim based on specified simple tracking code...
	 * 
	 * @param simpleTrackingCode
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	Pair<String, List<DataExchangeJounalEntry>> extractRequestStatusBySimpleTrackingCode(String simpleTrackingCode)
			throws IOException, ClientProtocolException;

}
