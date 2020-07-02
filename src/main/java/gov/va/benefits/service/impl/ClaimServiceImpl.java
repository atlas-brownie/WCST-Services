package gov.va.benefits.service.impl;

import org.springframework.stereotype.Service;

import gov.va.benefits.dto.ClaimsDetails;
import gov.va.benefits.dto.ClaimsStatusResponse;
import gov.va.benefits.service.ClaimsService;

@Service
public class ClaimServiceImpl implements ClaimsService {

	@Override
	public ClaimsStatusResponse submitClaim(ClaimsDetails aClaimsDetails) {
		return null;
	}
}
