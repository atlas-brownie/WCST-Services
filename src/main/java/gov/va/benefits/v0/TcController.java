package gov.va.benefits.v0;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import gov.va.benefits.dto.ClaimsDetails;
import gov.va.benefits.dto.PayloadWrapper;
import gov.va.benefits.service.ClaimsService;

@Deprecated
/**
 * Deprecated - Start using the versions of the controller defined in packages
 * com.unisys.tc.controller.v1
 */
@RestController
@CrossOrigin(origins = "*")
public class TcController {
	@Autowired
	ClaimsService itcService;

	// This method is used to test database insert
	@PostMapping("/submitResponse")
	public PayloadWrapper<String> submitTcResponse(@RequestBody ClaimsDetails details) {
		itcService.submitClaim(details);
		PayloadWrapper<String> payloadWrapper = new PayloadWrapper<String>("Saved Successfully");
		return payloadWrapper;
	}

}
