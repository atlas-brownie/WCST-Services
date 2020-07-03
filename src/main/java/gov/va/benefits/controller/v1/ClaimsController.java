package gov.va.benefits.controller.v1;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gov.va.benefits.dto.ClaimDetails;
import gov.va.benefits.dto.ClaimStatusResponse;
import gov.va.benefits.dto.PayloadWrapper;
import gov.va.benefits.service.ClaimsService;

/**
 * 
 * @author Laljith Antony
 * 
 *         Spring MVC controller that exposes REST-end points to submit claims
 *         and also monitoring statuses of submitted claims...
 *
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class ClaimsController {
	@Autowired
	private SmartValidator validator;

	@Autowired
	private ClaimsService claimsService;

	@PutMapping(path = "/claims", headers = "content-type=multipart/form-data")
	public PayloadWrapper<ClaimStatusResponse> uploadClaim(@RequestPart String firstName, @RequestPart String lastName,
			@RequestPart String zipCode, @RequestPart String ssn, @RequestPart MultipartFile claimsFile,
			BindingResult aResult) throws IOException {
		ClaimDetails claimsDetail = new ClaimDetails();
		claimsDetail.setFirstName(firstName);
		claimsDetail.setLastName(lastName);
		claimsDetail.setZipCode(zipCode);
		claimsDetail.setSsn(ssn);
		claimsDetail.setClaimFile(claimsFile);

		validator.validate(claimsDetail, aResult);

		if (aResult.hasErrors()) {
			ClaimStatusResponse errorResponse = new ClaimStatusResponse();
			return extractValidationErrors(errorResponse, aResult);
		}

		ClaimStatusResponse statusResponse = null;
		try {
			statusResponse = claimsService.processClaimRequest(claimsDetail);
		} catch (Exception exp) {
			ClaimStatusResponse errorResponse = new ClaimStatusResponse();
			return extractValidationErrors(errorResponse, aResult, exp.getMessage());
		}

		PayloadWrapper<ClaimStatusResponse> responsePayload = new PayloadWrapper<>(statusResponse);

		return responsePayload;
	}

	private <T extends Serializable> PayloadWrapper<T> extractValidationErrors(T payload, BindingResult result) {
		return extractValidationErrors(payload, result, "One or more errors found!");
	}

	private <T extends Serializable> PayloadWrapper<T> extractValidationErrors(T payload, BindingResult result,
			String errorMessage) {
		PayloadWrapper<T> payloadWrapper = new PayloadWrapper<>(payload);

		payloadWrapper.setHasError(true);

		payloadWrapper.setMessage(errorMessage);

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
