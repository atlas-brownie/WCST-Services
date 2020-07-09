package gov.va.benefits.domain;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class ClaimRecord implements Serializable {
	private static final long serialVersionUID = -4639348740237743929L;

	private String id;

	private String firstName;

	private String lastName;

	private String zipCode;

	private String ssn;

	private String claimFileName;

	private byte[] claimFileContent;

	private String vaFileLocation;

	private String vaTrackerCode;

	private String simpleTrackingCode;

	private Date submissionDate;

	private String currentStatus;

	private int numberRetries;

	private Date lastUpdatedDate;
}
