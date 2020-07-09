package gov.va.benefits.service.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.ContainerCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;

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

	@Value("${claimMetaDataTable}")
	private String dynamoDBTableName;

	@Value("${persistClaimMetaData:true}")
	private boolean persistClaimMetaData;
	
	@Value("${localDB:false}")
	private boolean localDB;
	
	@Value("${localDBEndPoint:testURL}")
	private String localDBEndPoint;

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
		
		if (persistClaimMetaData) {
			if(localDB) {
				LOGGER.debug("*******Local Dynamo DB configuration **********");
				AWSCredentials credential = new BasicAWSCredentials("Dummy", "Dummy");
				dynamoDB = new AmazonDynamoDBClient(credential);
				dynamoDB.setRegion(Region.getRegion(Regions.US_EAST_1));
				dynamoDB.setEndpoint(localDBEndPoint);				
				createLocalDynamoTable();

			} else {
				LOGGER.debug("*******AWS Dynamo DB configuration **********");
				AWSCredentialsProvider provider = new ContainerCredentialsProvider();
				dynamoDB = new AmazonDynamoDBClient(provider);
				dynamoDB.setRegion(Region.getRegion(Regions.US_EAST_1));
			}
		}		

	}
	
	private void createLocalDynamoTable() {
		
		 
		KeySchemaElement keyElement = new KeySchemaElement();
		keyElement.setAttributeName(ID_COLUMN);
		keyElement.setKeyType(KeyType.HASH);
		
		List<AttributeDefinition> attributeDefinitions= new ArrayList<AttributeDefinition>();
		attributeDefinitions.add(new AttributeDefinition().withAttributeName(ID_COLUMN).withAttributeType("S"));

		
		CreateTableRequest request
		= new CreateTableRequest()
		.withTableName(dynamoDBTableName)
		.withKeySchema(keyElement)
		.withAttributeDefinitions(attributeDefinitions)
		.withProvisionedThroughput(new ProvisionedThroughput()
	            .withReadCapacityUnits(5L)
	            .withWriteCapacityUnits(6L));
		
		try {
			dynamoDB.describeTable(dynamoDBTableName);
		} catch (final ResourceNotFoundException e) {
			LOGGER.info("Dynamo db table {} doesn't exist, attempting to create....", dynamoDBTableName);
			dynamoDB.createTable(request);
		}
	
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
		LOGGER.debug("begin saveClaimDetails()...");

		if (!persistClaimMetaData) {
			aClaimRecord.setSimpleTrackingCode(aClaimRecord.getVaTrackerCode());

			return aClaimRecord;
		}

		// Make change here later so that instead of using a generated UUID as the ID
		// value for the record use simple tracking code and implement collision
		// resolution mechanism...
		String recordId = UUID.randomUUID().toString();

		if (StringUtils.isEmpty(aClaimRecord.getSimpleTrackingCode())) {
			aClaimRecord.setSimpleTrackingCode(generateSimpleTrackingCode(aClaimRecord));
		}

		DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateTimeStr = dateFmt.format(aClaimRecord.getSubmissionDate());
	
		Item item = new Item()
				.withPrimaryKey("id", recordId)
				.withString("firstName", aClaimRecord.getFirstName())
				.withString("lastName", aClaimRecord.getLastName())
				.withString("zipCode", aClaimRecord.getZipCode())
				.withString("simpleTrackingCode", aClaimRecord.getSimpleTrackingCode())
				.withString("currentStatus", aClaimRecord.getCurrentStatus())
				.withString("vaFileLocation", aClaimRecord.getVaFileLocation())
				.withString("vaTrackerCode", aClaimRecord.getVaTrackerCode())
				.withString("claimFileName", aClaimRecord.getClaimFileName())
				.withString("submissionDate", dateTimeStr)
				.withString("lastUpdatedDate", dateTimeStr)
				.withString("vaTrackerCode", aClaimRecord.getVaTrackerCode());

		DynamoDB db = new DynamoDB(dynamoDB);
		Table table = db.getTable(dynamoDBTableName);
		LOGGER.debug("Saving .......Record [{}] in DB.", recordId);
		table.putItem(item);
		LOGGER.debug("Saved Record [{}] in DB.", recordId);

		aClaimRecord.setId(recordId);

		LOGGER.debug("end saveClaimDetails()...");

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
		String simpleTrackingCode = aSearchCriteria.get("simpleTrackingCode");
		Set<ClaimRecord> claimRecordSet = new HashSet<ClaimRecord>();
		ClaimRecord claimRecord = new ClaimRecord();		
		try {			
			Map<String, Object> expressionAttributeValues = new HashMap<String, Object>(); 
			expressionAttributeValues.put(":simpleTrackingCode", simpleTrackingCode); 		   
			DynamoDB db = new DynamoDB(dynamoDB);
			Table table = db.getTable(dynamoDBTableName);
			LOGGER.debug("retrieving record by short code {}", simpleTrackingCode);
			ItemCollection<ScanOutcome> items = table.scan ( 
					"simpleTrackingCode = :simpleTrackingCode",                                 
					null,null,                                           
					expressionAttributeValues);			         
			Iterator<Item> iterator = items.iterator(); 				
			while (iterator.hasNext()) {
				Item item = iterator.next();
				LOGGER.debug("Retrieved record VA code {}",item.getString("vaTrackerCode"));
				claimRecord.setVaTrackerCode(item.getString("vaTrackerCode"));
				claimRecordSet.add(claimRecord);
			}
		} catch (Exception e) {
			LOGGER.error("Exception retireving  record {}" , simpleTrackingCode, e);
		}

		return claimRecordSet;
	}

}
