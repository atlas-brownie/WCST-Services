package gov.va.benefits.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class PayloadWrapper<T> {
	private LocalDateTime dateTime = LocalDateTime.now();
	private int length;
	private String payloadType;
	private List<T> payload = new ArrayList<T>();
	private boolean hasError = false;
	private String message = "Success";

	private Map<String, String> errorMap;

	public PayloadWrapper(T obj) {
		this.payloadType = obj == null ? "NONE" : obj.getClass().getName();
		this.payload.add(obj);
		this.length = payload.size();
	}

	public PayloadWrapper(List<T> list) {
		payloadType = list == null ? "NONE" : list.getClass().getName();
		payload.addAll(list);
		length = payload.size();
	}

	public PayloadWrapper() {

	}
}
