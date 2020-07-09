package gov.va.benefits.controller;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import gov.va.benefits.controller.ExceptionHandlerController;
import gov.va.benefits.dto.PayloadWrapper;

public class ExceptionHandlerControllerTest {

	private static final String TEST_EXCEPTION = "Test Exception";

	@Test
	public void testHandleConstraintViolationException() {
		ExceptionHandlerController exceptionHandler = new ExceptionHandlerController();
		DataIntegrityViolationException exp = new DataIntegrityViolationException(TEST_EXCEPTION);
		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "testUrl");
		PayloadWrapper<Void> payLoad = exceptionHandler.handleConstraintViolationException(exp, request);

		assertEquals("Invalid Response!", TEST_EXCEPTION, payLoad.getMessage());
	}

	@Test
	public void testHandleError() {
		ExceptionHandlerController exceptionHandler = new ExceptionHandlerController();
		Exception exp = new Exception(TEST_EXCEPTION);
		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "testUrl");

		PayloadWrapper<Void> payLoad = exceptionHandler.handleError(request, exp);
		assertEquals("Invalid Response!", "Unexpected error occured!", payLoad.getMessage());
	}

}
