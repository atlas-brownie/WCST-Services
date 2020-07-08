package gov.va.benefits.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import lombok.Data;

@Data
public class DataExchangeJounalEntry implements Serializable {
	private static final long serialVersionUID = -4639348740237743929L;

	private String targetUrl;

	private Map<String, String> attributeMap;

	private String responseCode;

	private Date requestSentTime;

	private Date responseReceivedTime;
}
