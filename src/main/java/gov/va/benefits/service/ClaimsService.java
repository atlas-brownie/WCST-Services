package gov.va.benefits.service;

import gov.va.benefits.dto.ClaimsDetails;
import gov.va.benefits.dto.ClaimsStatusResponse;

public interface ClaimsService {

	public ClaimsStatusResponse submitClaim(ClaimsDetails responseDetails);

}
