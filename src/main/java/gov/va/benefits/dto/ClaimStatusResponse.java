package gov.va.benefits.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class ClaimStatusResponse implements Serializable {
	private static final long serialVersionUID = -4639348740237743929L;

	private String firstName;

	private String lastName;

	private String trackingCode;

	private String vaTrackingCode;

	private String claimStatus;

	private Date submissionDate;

}
