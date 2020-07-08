package gov.va.benefits.utils;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

/**
 * Spring singleton bean class that implements a closeable HTTP client...
 * 
 * @author L Antony
 *
 */
@Component
public class HttpClientBean {
	public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler)
			throws IOException, ClientProtocolException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		return httpclient.execute(request, responseHandler);
	}
}
