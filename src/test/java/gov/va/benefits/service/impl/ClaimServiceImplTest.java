package gov.va.benefits.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.ValidationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import gov.va.benefits.domain.ClaimRecord;
import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;
import gov.va.benefits.service.CSPInterfaceService;
import gov.va.benefits.utils.HttpClientBean;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class ClaimServiceImplTest {
	@Mock
	private CSPInterfaceService cspService;

	@Mock
	private HttpClientBean httpClientBean;

	@InjectMocks
	private ClaimServiceImpl claimServiceImpl;

	@Before
	public void setUp() throws ClientProtocolException, IOException, JSONException {
		final String mockETagValue = RandomStringUtils.randomNumeric(30);
		final String mockLocation = "{\r\n" + "    \"data\": {\r\n" + "\r\n" + "        \"attributes\": {\r\n"
				+ "            \"guid\": \"3ff64dcc-1fea-4984-890f-d2e75643127d\",\r\n"
				+ "            \"location\": \"http://mockurl\"\r\n" + "        }\r\n" + "    }\r\n" + "}";

		final String mockStatus = "{\r\n" + "    \"data\": {\r\n" + "\r\n" + "        \"attributes\": {\r\n"
				+ "            \"guid\": \"3ff64dcc-1fea-4984-890f-d2e75643127d\",\r\n"
				+ "            \"status\": \"pending\"\r\n" + "        }\r\n" + "    }\r\n" + "}";

		Mockito.when(httpClientBean.execute(Mockito.any(), Mockito.any())).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				if (invocation.getArgument(0) instanceof HttpPost) {
					return mockLocation;
				}

				if (invocation.getArgument(0) instanceof HttpPut) {
					return mockETagValue;
				}

				// For HttpGet...
				return mockStatus;
			}
		});
		ClaimRecord mockClaimRec = new ClaimRecord();
		mockClaimRec.setVaTrackerCode(RandomStringUtils.randomAlphanumeric(32));

		Mockito.when(cspService.findClaimRecord(Mockito.any())).thenReturn(Collections.singleton(mockClaimRec));

		ReflectionTestUtils.setField(claimServiceImpl, "claimsIntakePointerUrl", "http://claimsIntakePointerUrl");
		ReflectionTestUtils.setField(claimServiceImpl, "vaAuthHeaderKey", "apiKey");
		ReflectionTestUtils.setField(claimServiceImpl, "vaAuthHeaderValue", "vaAuthHeaderValue");

		ReflectionTestUtils.setField(claimServiceImpl, "clientSystemId", "clientSystemId");
		ReflectionTestUtils.setField(claimServiceImpl, "sourceDocumentType", "sourceDocumentType");
	}

	@Test
	public void testProcessClaimRequest() throws IOException {
		ClaimDetails claimDetails = new ClaimDetails();
		claimDetails.setFirstName(RandomStringUtils.randomAlphabetic(10));
		claimDetails.setLastName(RandomStringUtils.randomAlphabetic(10));
		claimDetails.setSsn(RandomStringUtils.randomNumeric(9));
		claimDetails.setZipCode(RandomStringUtils.randomNumeric(5));
		claimDetails.setClaimFileName(String.format("%s.pdf", RandomStringUtils.randomAlphabetic(8)));
		claimDetails.setClaimeFileContent(RandomStringUtils.randomAlphanumeric(1000).getBytes());

		ClaimStatusResponse statusResponse = claimServiceImpl.processClaimRequest(claimDetails);

		assertEquals("Invalid Last Name!", claimDetails.getLastName(), statusResponse.getLastName());

		assertNotNull("Missing Claim Status!", statusResponse.getClaimStatus());

		Mockito.verify(httpClientBean, Mockito.times(3)).execute(Mockito.any(), Mockito.any());
	}

	@Test(expected = ValidationException.class)
	public void testProcessClaimRequestNegative() throws IOException {
		ClaimDetails claimDetails = new ClaimDetails();

		claimServiceImpl.processClaimRequest(claimDetails);
	}

	@Test
	public void testExtractRequestStatusBySimpleTrackingCode() throws ClientProtocolException, IOException {
		String mockSimpleTrackingCode = RandomStringUtils.randomAlphanumeric(32);

		String statusResponse = claimServiceImpl.extractRequestStatusBySimpleTrackingCode(mockSimpleTrackingCode);

		assertNotNull("Missing Claim Status!", statusResponse);
		Mockito.verify(httpClientBean).execute(Mockito.any(HttpGet.class), Mockito.any());
	}

	@Test
	public void testExtractRequestStatusByVaTrackingNumber() throws ClientProtocolException, IOException {
		String mockVATrackingNumber = RandomStringUtils.randomAlphanumeric(32);

		String statusResponse = claimServiceImpl.extractRequestStatusByVaTrackingNumber(mockVATrackingNumber);

		assertNotNull("Missing Claim Status!", statusResponse);
	}

}
