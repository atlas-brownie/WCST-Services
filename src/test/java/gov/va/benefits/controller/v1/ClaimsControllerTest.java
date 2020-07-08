package gov.va.benefits.controller.v1;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.validation.BindingResult;

import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;
import gov.va.benefits.dto.DataExchangeJounalEntry;
import gov.va.benefits.service.ClaimsService;

@RunWith(SpringRunner.class)
@WebMvcTest(ClaimsController.class)
public class ClaimsControllerTest {
	@Autowired
	private MockMvc mockClaimsController;

	@MockBean
	private ClaimsService claimsService;

	@MockBean
	private BindingResult bindingResult;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testUploadClaim() throws Exception {
		ClaimStatusResponse stausResponse = new ClaimStatusResponse();

		Mockito.when(claimsService.processClaimRequest(Mockito.any(ClaimDetails.class))).thenReturn(stausResponse);

		MockMultipartFile contentFile = new MockMultipartFile("claimFile", "claim_form.pdf", "application/pdf",
				RandomStringUtils.random(80).getBytes());

		mockClaimsController.perform(MockMvcRequestBuilders.multipart("/api/v1/uploads").file(contentFile)
				.param("firstName", RandomStringUtils.randomAlphabetic(30))
				.param("lastName", RandomStringUtils.randomAlphabetic(30))
				.param("zipCode", RandomStringUtils.randomNumeric(5)).param("ssn", RandomStringUtils.randomNumeric(9)))
				.andExpect(MockMvcResultMatchers.status().is(200));

	}

	@Test
	public void testGetClaimStatus() throws Exception {
		String simpleTrackingCode = RandomStringUtils.randomAlphabetic(10);

		String statusStr = RandomStringUtils.randomAlphabetic(10);
		List<DataExchangeJounalEntry> entryList = new ArrayList<>();

		Pair<String, List<DataExchangeJounalEntry>> results = new ImmutablePair<>(statusStr, entryList);

		Mockito.when(claimsService.extractRequestStatusBySimpleTrackingCode(simpleTrackingCode)).thenReturn(results);

		mockClaimsController.perform(MockMvcRequestBuilders.get("/api/v1/uploads/" + simpleTrackingCode))
				.andExpect(MockMvcResultMatchers.status().is(200))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void testGetClaimStatusByVaTrackingCode() throws Exception {
		String vaTrackingNumber = RandomStringUtils.randomAlphabetic(10);
		String statusStr = RandomStringUtils.randomAlphabetic(10);

		List<DataExchangeJounalEntry> entryList = new ArrayList<>();

		Pair<String, List<DataExchangeJounalEntry>> results = new ImmutablePair<>(statusStr, entryList);
		Mockito.when(claimsService.extractRequestStatusByVaTrackingNumber(vaTrackingNumber)).thenReturn(results);

		mockClaimsController.perform(MockMvcRequestBuilders.get("/api/v1/uploads/va" + vaTrackingNumber))
				.andExpect(MockMvcResultMatchers.status().is(200))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

}
