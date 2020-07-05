package gov.va.benefits.service;

import java.io.IOException;

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

}
