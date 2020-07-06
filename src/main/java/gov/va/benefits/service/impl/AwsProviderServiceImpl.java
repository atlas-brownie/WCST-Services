package gov.va.benefits.service.impl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import gov.va.benefits.domain.ClaimRecord;
import gov.va.benefits.service.CSPInterfaceService;

/**
 * AWS Specific Implementation of CSP Services Interfaces...
 * 
 * @author L Antony
 *
 */
@Service
public class AwsProviderServiceImpl implements CSPInterfaceService {
	private static Logger LOGGER = LoggerFactory.getLogger(ClaimServiceImpl.class);

	private static final String ID_COLUMN = "id";

	@Value("${claimMetaDataTable:ClaimMetaData}")
	private String dynamoDBTableName;

	@Value("${awsAccessKey:AKIAQYWCIK5QRGOXCAXO}")
	private String awsAccessKey;

	@Value("${awsSecretKey:peP+JMJiprSioigCAfpXkQQhHpyw9nRicdelqSEk}")
	private String awsSecretKey;

	private AmazonDynamoDB dynamoDB;

	private static long BASE_TIME_IN_SEC = 0;

	static {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.YEAR, 2020);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DAY_OF_MONTH, 1);

		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		BASE_TIME_IN_SEC = cal.getTimeInMillis() / 1000;
	}

	@PostConstruct
	public void initBean() {
		BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
		dynamoDB = new AmazonDynamoDBClient(credentials);
		dynamoDB.setRegion(Region.getRegion(Regions.US_EAST_1));
	}

	@PreDestroy
	public void destry() {
		dynamoDB.shutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.va.benefits.service.CSPInterfaceService#saveClaimDetails(gov.va.benefits.
	 * domain.ClaimRecord)
	 */
	@Override
	public ClaimRecord saveClaimDetails(ClaimRecord aClaimRecord) {
		String recordId = UUID.randomUUID().toString();

		if (StringUtils.isNotBlank(aClaimRecord.getSimpleTrackingCode())) {
			aClaimRecord.setSimpleTrackingCode(generateSimpleTrackingCode(aClaimRecord));
		}

		Map<String, AttributeValue> itemRec = new HashMap<String, AttributeValue>();

		itemRec.put(ID_COLUMN, new AttributeValue(recordId));
		itemRec.put("firstName", new AttributeValue(aClaimRecord.getFirstName()));
		itemRec.put("lastName", new AttributeValue(aClaimRecord.getLastName()));
		itemRec.put("zipCode", new AttributeValue(aClaimRecord.getZipCode()));
		itemRec.put("ssn", new AttributeValue(aClaimRecord.getSsn()));

		itemRec.put("simpleTrackingCode", new AttributeValue(aClaimRecord.getSimpleTrackingCode()));
		itemRec.put("currentStatus", new AttributeValue(aClaimRecord.getCurrentStatus()));

		itemRec.put("vaFileLocation", new AttributeValue(aClaimRecord.getVaFileLocation()));
		itemRec.put("vaTrackerCode", new AttributeValue(aClaimRecord.getVaTrackerCode()));

		itemRec.put("claimFileName", new AttributeValue(aClaimRecord.getClaimFileName()));
		itemRec.put("claimFileName", new AttributeValue(aClaimRecord.getClaimFileName()));

		DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateTimeStr = dateFmt.format(aClaimRecord.getSubmissionDate());
		itemRec.put("submissionDate", new AttributeValue(dateTimeStr));

		if (aClaimRecord.getLastUpdatedDate() == null) {
			aClaimRecord.setLastUpdatedDate(new Date());
		}

		dateTimeStr = dateFmt.format(aClaimRecord.getLastUpdatedDate());
		itemRec.put("lastUpdatedDate", new AttributeValue(dateTimeStr));

		dynamoDB.putItem(dynamoDBTableName, itemRec);

		aClaimRecord.setId(recordId);

		return aClaimRecord;
	}

	/**
	 * Generates user-friendly tracking code...
	 * 
	 * Please note that this method doesn't take into consideration multiple
	 * simultaneous instances of cluster nodes and no instance id related attributes
	 * are taken into consideration in generating tracking code for simplicity.
	 * Therefore in rare instance there could be collisions in generated tracking
	 * codes...
	 * 
	 * @param aClaimRecord
	 * @return
	 */
	private String generateSimpleTrackingCode(ClaimRecord aClaimRecord) {
		long currentTimeSec = System.currentTimeMillis() / 1000;

		String trackingNumber = StringUtils
				.upperCase(String.format("%s%s%4s-%s", StringUtils.substring(aClaimRecord.getFirstName(), 0, 1),
						StringUtils.substring(aClaimRecord.getLastName(), 0, 1),
						StringUtils.substring(aClaimRecord.getSsn(), StringUtils.length(aClaimRecord.getSsn()) - 4,
								StringUtils.length(aClaimRecord.getSsn())),
						StringUtils.leftPad(Long.toHexString(currentTimeSec - BASE_TIME_IN_SEC), 6, '0')));

		LOGGER.debug("Tracking Code:" + trackingNumber);

		return trackingNumber;
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
		Map<String, AttributeValue> queryAttributes = new HashMap<String, AttributeValue>();
		queryAttributes.put(ID_COLUMN, new AttributeValue(aId));
		GetItemRequest itemRequest = new GetItemRequest(dynamoDBTableName, queryAttributes);

		GetItemResult itemResult = dynamoDB.getItem(itemRequest);
		Map<String, AttributeValue> itemAttributes = itemResult.getItem();
		if (itemAttributes == null || itemAttributes.isEmpty()) {
			return Optional.empty();
		}

		ModelMapper mapper = new ModelMapper();
		ClaimRecord result = mapper.map(itemResult.getItem(), ClaimRecord.class);

		return Optional.of(result);
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
