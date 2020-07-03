package gov.va.benefits.service;

import java.io.IOException;

import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;

/**
 * 
 * @author Laljith Antony
 * 
 *         Service responsible for uploading and monitoring of VA claims
 *         processing...
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
	public ClaimStatusResponse processClaimRequest(ClaimDetails aClaimDetails) throws IOException;

}
