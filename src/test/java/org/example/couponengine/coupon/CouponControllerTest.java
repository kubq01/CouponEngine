package org.example.couponengine.coupon;

import jakarta.persistence.EntityManager;
import org.example.couponengine.BaseIntegrationTest;
import org.example.couponengine.coupon.persistence.CouponEntity;
import org.example.couponengine.coupon.persistence.CouponRepository;
import org.example.couponengine.coupon.persistence.CouponUsageRepository;
import org.example.couponengine.geo.GeoMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.utility.Base58.randomString;

class CouponControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponUsageRepository couponUsageRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private GeoMappingService geoService;

    @BeforeEach
    public void setup() {
        couponRepository.deleteAll();
        couponUsageRepository.deleteAll();
        when(geoService.getCountryCode(anyString()))
                .thenReturn("PL");
    }

    @Nested
    class CreateCoupon {

        @Test
        void shouldCreateCouponAndSaveInDatabase() throws Exception {
            //when
            String request = createCouponCreateRequest("spring", Instant.now(), 10, 0, "PL");

            //then
            mockMvc.perform(post(createCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isNoContent());

            var saved = couponRepository.findAll();

            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getId()).isEqualTo("spring");
        }

        @Test
        void shouldReturnBadRequestWhenCouponIdIsInvalid() throws Exception {
            //when
            String request = createCouponCreateRequest("invalid-id!", Instant.now(), 10, 0, "PL");

            //then
            mockMvc.perform(post(createCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnBadRequestWhenCountryIdIsInvalid() throws Exception {
            //when
            String request = createCouponCreateRequest("spring", Instant.now(), 10, 0, "invalid-country-code");

            //then
            mockMvc.perform(post(createCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectDuplicateCouponId() throws Exception {
            //when
            String request1 = createCouponCreateRequest("spring", Instant.now(), 10, 0, "PL");
            String request2 = createCouponCreateRequest("SPRING", Instant.now(), 10, 0, "PL");

            //then
            mockMvc.perform(post(createCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request1))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post(createCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request2))
                    .andExpect(status().isConflict());

            assertThat(couponRepository.findAll()).hasSize(1);
        }
    }

    @Nested
    class RedeemCoupon {

        @Test
        void shouldReturnNotFoundWhenCouponDoesNotExist() throws Exception {
            //when
            String request = createRedeemCouponRequest("spring", "appUser");

            //then
            mockMvc.perform(post(redeemCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(result ->
                            assertThat(result.getResponse().getContentAsString())
                                    .contains("COUPON_NOT_FOUND_FAILURE")
                    );
        }

        @Test
        void shouldThrowErrorForInvalidCountry() throws Exception {
            //when
            couponRepository.saveAndFlush(new CouponEntity(
                    "spring",
                    Instant.now(),
                    10,
                    0,
                    "UK"
            ));

            String request = createRedeemCouponRequest("spring", "appUser");

            //then
            mockMvc.perform(post(redeemCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(result ->
                            assertThat(result.getResponse().getContentAsString())
                                    .contains("INVALID_COUNTRY_FAILURE")
                    );

            entityManager.flush();
            entityManager.clear();

            var updated = couponRepository.findById("spring").orElseThrow();
            assertThat(updated.getCurrentUsages()).isEqualTo(0);
        }

        @Test
        void shouldReturnBadRequestForInvalidUserId() throws Exception {
            //when

            String request = createRedeemCouponRequest("spring", "invalid-user-id!");

            //then
            mockMvc.perform(post(redeemCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldSuccessfullyRedeemCoupon() throws Exception {
            //when
            couponRepository.saveAndFlush(new CouponEntity(
                    "spring",
                    Instant.now(),
                    10,
                    0,
                    "PL"
            ));

            String request = createRedeemCouponRequest("spring", "appUser");

            //then
            mockMvc.perform(post(redeemCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(result ->
                            assertThat(result.getResponse().getContentAsString())
                                    .contains("SUCCESS")
                    );

            entityManager.flush();
            entityManager.clear();

            var updated = couponRepository.findById("spring").orElseThrow();
            assertThat(updated.getCurrentUsages()).isEqualTo(1);
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void shouldRejectWhenMaxUsagesReached() throws Exception {
            //when
            couponRepository.saveAndFlush(new CouponEntity(
                    "spring",
                    Instant.now(),
                    1,
                    1,
                    "PL"
            ));

            String request = createRedeemCouponRequest("spring", "appUser");

            //then
            mockMvc.perform(post(redeemCouponUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(result ->
                            assertThat(result.getResponse().getContentAsString())
                                    .contains("MAX_USAGES_REACHED_FAILURE")
                    );

            var updated = couponRepository.findById("spring").orElseThrow();
            assertThat(updated.getCurrentUsages()).isEqualTo(1);
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void shouldAllowOnlyOneUsageOfCouponPeruser() {
            //when
            couponRepository.saveAndFlush(new CouponEntity(
                    "spring",
                    Instant.now(),
                    2,
                    0,
                    "PL"
            ));

            String request = createRedeemCouponRequest("spring", "appUser");

            int requests = 2;

            //then
            List<String> responses = IntStream.range(0, requests)
                    .parallel()
                    .mapToObj(i -> {
                        try {
                            return mockMvc.perform(post(redeemCouponUrl())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(request))
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            long successCount = responses.stream()
                    .filter(r -> r.contains("SUCCESS"))
                    .count();

            long failureCount = responses.stream()
                    .filter(r -> r.contains("COUPON_ALREADY_USED_BY_USER_FAILURE"))
                    .count();

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(1);

            var updated = couponRepository.findById("spring").orElseThrow();
            assertThat(updated.getCurrentUsages()).isEqualTo(1);
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void shouldAllowOnlyMaxSuccessfulUsesUnderHighConcurrency() {
            //when
            couponRepository.saveAndFlush(new CouponEntity(
                    "spring",
                    Instant.now(),
                    1,
                    0,
                    "PL"
            ));

            int requests = 20;

            //then
            List<String> responses = IntStream.range(0, requests)
                    .parallel()
                    .mapToObj(i -> {
                        try {
                            return mockMvc.perform(post(redeemCouponUrl())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(generateRedeemCouponRequestWithRandomUserId("spring")))
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            long successCount = responses.stream()
                    .filter(r -> r.contains("SUCCESS"))
                    .count();

            long failureCount = responses.stream()
                    .filter(r -> r.contains("MAX_USAGES_REACHED_FAILURE"))
                    .count();

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(requests - 1);

            var updated = couponRepository.findById("spring").orElseThrow();
            assertThat(updated.getCurrentUsages()).isEqualTo(1);
        }
    }

    private String generateRedeemCouponRequestWithRandomUserId(String couponId) {
        return createRedeemCouponRequest(couponId, randomString(16));
    }

    private String createRedeemCouponRequest(String couponId, String userId) {
        return """
        {
                "couponId": "%s",
                "userId": "%s"
        }
        """.formatted(couponId, userId);
    }

    private String createCouponCreateRequest(String id, Instant createdAt, int maxUsages,
                                             int currentUsages, String countryCode) {
        return """
            {
              "id": "%s",
              "createdAt": "%s",
              "maxUsages": %s,
              "currentUsages": %s,
              "countryCode": "%s"
            }
            """.formatted(id, createdAt.toString(), maxUsages, currentUsages, countryCode);
    }

    private String redeemCouponUrl() {
        return "/coupon/redeem";
    }

    private String createCouponUrl() {
        return "/coupon/create";
    }
}