package com.turbosql.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.turbosql.TurboSqlApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = TurboSqlApplication.class)
@AutoConfigureMockMvc
class SqlAnalysisControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void analyzeReturnsJsonReport() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dialect\":\"postgresql\",\"sql\":\"SELECT id FROM employee WHERE id = 1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success", is(true)))
        .andExpect(jsonPath("$.ast.type", is("STATEMENT_SELECT")))
        .andExpect(jsonPath("$.estimatedCost.totalCost").exists());
  }

  @Test
  void syntaxErrorsUseErrorEnvelope() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dialect\":\"postgresql\",\"sql\":\"SELECT FROM employee\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success", is(false)))
        .andExpect(jsonPath("$.error.code", is("SQL_SYNTAX_ERROR")))
        .andExpect(jsonPath("$.error.message", containsString("Unexpected token FROM")));
  }
}
