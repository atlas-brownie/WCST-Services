package gov.va.benefits.service;

import java.io.IOException;
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

	void purgeFileContent(ClaimRecord aClaimRecord) throws IOException;

	void purgeFileContent(String aClaimRecordId) throws IOException;

	Optional<ClaimRecord> findClaimRecordById(String aId);

	Set<ClaimRecord> findClaimRecord(Map<String, String> aSearchCriteria);
}
