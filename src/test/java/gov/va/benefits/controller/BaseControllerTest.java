package gov.va.benefits.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import gov.va.benefits.controller.BaseController;

@RunWith(SpringRunner.class)
@WebMvcTest(BaseController.class)
public class BaseControllerTest {
	@Autowired
	private MockMvc mvc;

	@Test
	public void index() throws Exception {
		mvc.perform(get("/")).andExpect(status().is3xxRedirection());
	}
}
