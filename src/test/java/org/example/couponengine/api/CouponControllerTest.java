package org.example.couponengine.api;

import org.example.couponengine.BaseIntegrationTest;
import org.example.couponengine.database.CouponRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateCouponAndSaveInDatabase() throws Exception {
        String json = """
            {
              "id": "spring",
              "createdAt": "2026-05-23T10:00:00Z",
              "maxUsages": 10,
              "currentUsages": 0,
              "countryCode": "PL"
            }
            """;

        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());

        var saved = repository.findAll();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isEqualTo("spring");
    }

    @Test
    void shouldReturnBadRequestWhenCouponIdIsInvalid() throws Exception {

        String json = """
            {
              "id": "invalid-id!",
              "createdAt": "2026-05-23T10:00:00Z",
              "maxUsages": 10,
              "currentUsages": 0,
              "countryCode": "PL"
            }
            """;

        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateCouponId() throws Exception {

        String c1 = """
            {
              "id": "spring",
              "createdAt": "2026-05-23T10:00:00Z",
              "maxUsages": 10,
              "currentUsages": 0,
              "countryCode": "PL"
            }
            """;

        String c2 = """
            {
              "id": "spring",
              "createdAt": "2026-05-23T10:00:00Z",
              "maxUsages": 10,
              "currentUsages": 0,
              "countryCode": "PL"
            }
            """;

        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c1))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c2))
                .andExpect(status().isConflict());

        assertThat(repository.findAll()).hasSize(1);
    }
}