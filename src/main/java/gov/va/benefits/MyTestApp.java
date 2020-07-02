package gov.va.benefits;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

public class MyTestApp {

	public static void main(String[] args) throws IOException {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		FileSystemResource value = new FileSystemResource(new File("/C:/Users/admin/Downloads/mat.jpg"));
		
		byte[] byteArray = IOUtils.toByteArray(value.getInputStream());
		
		map.add("file", byteArray);
		map.add("query", "tea");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		
		headers.setContentType(MediaType.MULTIPART_MIXED);
		
		headers.set("x-api-key", "GtJk9fqeCV5W99H7g65kH6S6pWLE0JBw51lYjYH3");
		
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange("https://aztj5hht4a.execute-api.us-east-1.amazonaws.com/dev/v3/multimodal?query=coffee", HttpMethod.POST, requestEntity, String.class);

		System.out.println("Result String:" + response.getBody());
	}

}
