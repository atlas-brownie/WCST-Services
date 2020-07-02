package gov.va.benefits.dto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ClaimsDetails implements Serializable {
	private static final long serialVersionUID = -4639348740237743929L;

	@NotNull
	@Size(min = 1, max = 80)
	private String firstName;

	@NotNull
	@Size(min = 3, max = 80)
	private String lastName;

	@NotNull
	private MultipartFile claimsFile;
}
