package gov.va.benefits.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import gov.va.benefits.domain.ClaimRecord;

/**
 * Service responsible for interfacing with Cloud Service Provider...
 * 
 * @author L Antony
 *
 */
public interface CSPInterfaceService {
	ClaimRecord saveClaimDetails(ClaimRecord aClaimRecord);

	Optional<ClaimRecord> findClaimRecordById(String aId);

	Set<ClaimRecord> findClaimRecord(Map<String, String> aSearchCriteria);
}
