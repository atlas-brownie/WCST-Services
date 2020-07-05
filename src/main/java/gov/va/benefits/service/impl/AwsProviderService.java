package gov.va.benefits.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import gov.va.benefits.domain.ClaimRecord;
import gov.va.benefits.service.CSPInterfaceService;

/**
 * AWS Specific Implementation of CSP Services Interfaces...
 * 
 * @author L Antony
 *
 */
@Service
public class AwsProviderService implements CSPInterfaceService {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.va.benefits.service.CSPInterfaceService#saveClaimDetails(gov.va.benefits.
	 * domain.ClaimRecord)
	 */
	@Override
	public ClaimRecord saveClaimDetails(ClaimRecord aClaimRecord) {
		// TODO Auto-generated method stub
		return aClaimRecord;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.va.benefits.service.CSPInterfaceService#purgeFileContent(gov.va.benefits.
	 * domain.ClaimRecord)
	 */
	@Override
	public void purgeFileContent(ClaimRecord aClaimRecord) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.va.benefits.service.CSPInterfaceService#purgeFileContent(java.lang.
	 * String)
	 */
	@Override
	public void purgeFileContent(String aClaimRecordId) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.va.benefits.service.CSPInterfaceService#findClaimRecordById(java.lang.
	 * String)
	 */
	@Override
	public Optional<ClaimRecord> findClaimRecordById(String aId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.va.benefits.service.CSPInterfaceService#findClaimRecord(java.util.Map)
	 */
	@Override
	public Set<ClaimRecord> findClaimRecord(Map<String, String> aSearchCriteria) {
		// TODO Auto-generated method stub
		return null;
	}

}
