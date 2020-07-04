package gov.va.benefits.dto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

/**
 * 
 * @author Laljith Antony
 *
 */
@Data
public class ClaimDetails implements Serializable {
	private static final long serialVersionUID = -4639348740237743929L;

	@NotNull
	@Size(min = 1, max = 80)
	private String firstName;

	@NotNull
	@Size(min = 3, max = 80)
	private String lastName;
	
	@NotNull
	@Size(min = 5, max = 10)
	private String zipCode;
	
	@NotNull
	@Size(min = 9, max = 11)
	private String ssn;

	@NotNull
	private MultipartFile claimFile;
}