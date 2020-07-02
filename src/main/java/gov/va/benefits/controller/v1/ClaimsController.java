package gov.va.benefits.controller.v1;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.va.benefits.dto.ClaimsDetails;
import gov.va.benefits.dto.ClaimsStatusResponse;
import gov.va.benefits.dto.PayloadWrapper;
import gov.va.benefits.service.ClaimsService;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class ClaimsController {
	@Autowired
	ClaimsService claimsService;

	@PutMapping("/claim")
	public PayloadWrapper<ClaimsStatusResponse> uploadClaim(@RequestBody @Valid ClaimsDetails aClaimsDetails,
			BindingResult aResult) throws IOException {
		if (aResult.hasErrors()) {
			ClaimsStatusResponse errorResponse = new ClaimsStatusResponse();
			return extractValidationErrors(errorResponse, aResult);
		}

		ClaimsStatusResponse statusResponse = claimsService.submitClaim(aClaimsDetails);

		PayloadWrapper<ClaimsStatusResponse> responsePayload = new PayloadWrapper<>(statusResponse);

		return responsePayload;

//		HttpHeaders headers = new HttpHeaders();
//		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
//		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//		LinkedMultiValueMap<String, Object> reqBody = new LinkedMultiValueMap<>();
//		reqBody.add("query", text);
//		if(imgFile != null) {
//			reqBody.add("file", imgFile.getResource());
//		}
//		HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(reqBody, headers);
//
//		RestTemplate restTemplate = new RestTemplate();
//
//		ResponseEntity<ResultsResponse> responseEntity = restTemplate.postForEntity(QUERY_BY_IMAGE_URI_DIRECT, httpEntity,
//				ResultsResponse.class);
//
//		ResultsResponse response = responseEntity.getBody();
//		
//
//		PayloadWrapper<Object> responsePayload = new PayloadWrapper<>(response.getResults());
//		responsePayload.setMetadata(response.getAggs());
//		return responsePayload;
	}

	private <T extends Serializable> PayloadWrapper<T> extractValidationErrors(T payload, BindingResult result) {
		PayloadWrapper<T> payloadWrapper = new PayloadWrapper<>(payload);

		payloadWrapper.setHasError(true);

		payloadWrapper.setMessage("One or more errors found!");

		Map<String, String> errorMap = new HashMap<>();
		result.getAllErrors().forEach(err -> {
			if (err instanceof FieldError) {
				FieldError fldErr = (FieldError) err;
				errorMap.put(String.format("%s", fldErr.getField()), fldErr.getDefaultMessage());
			} else {
				if (!errorMap.containsKey(err.getObjectName())) {
					errorMap.put(err.getObjectName(), err.getDefaultMessage());
				} else {
					String errors = errorMap.get(err.getObjectName()) + ",\n" + err.getDefaultMessage();
					errorMap.put(err.getObjectName(), errors);
				}

			}
		});

		payloadWrapper.setErrorMap(errorMap);

		return payloadWrapper;
	}
}
