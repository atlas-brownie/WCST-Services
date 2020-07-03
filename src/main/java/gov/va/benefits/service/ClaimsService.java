package gov.va.benefits.service;

import java.io.IOException;

import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;

/**
 * 
 * @author Laljith Antony
 *
 */
public interface ClaimsService {

	public ClaimStatusResponse processClaimRequest(ClaimDetails responseDetails) throws IOException;

}
