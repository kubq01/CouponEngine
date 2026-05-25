package org.example.couponengine.api;

import jakarta.persistence.EntityManager;
import org.example.couponengine.BaseIntegrationTest;
import org.example.couponengine.database.CouponEntity;
import org.example.couponengine.database.CouponRepository;
import org.example.couponengine.database.CouponUsageRepository;
import org.example.couponengine.geo.GeoMappingService;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void shouldCreateCouponAndSaveInDatabase() throws Exception {
        //when
        String json = createCouponCreateRequest("spring", Instant.now(), 10, 0, "PL");

        //then
        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());

        var saved = couponRepository.findAll();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isEqualTo("spring");
    }

    @Test
    void shouldReturnBadRequestWhenCouponIdIsInvalid() throws Exception {
        //when
        String json = createCouponCreateRequest("invalid-id!", Instant.now(), 10, 0, "PL");

        //then
        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCountryIdIsInvalid() throws Exception {
        //when
        String json = createCouponCreateRequest("spring", Instant.now(), 10, 0, "invalid-country-code");

        //then
        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateCouponId() throws Exception {
        //when
        String c1 = createCouponCreateRequest("spring", Instant.now(), 10, 0, "PL");
        String c2 = createCouponCreateRequest("SPRING", Instant.now(), 10, 0, "PL");

        //then
        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c1))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c2))
                .andExpect(status().isConflict());

        assertThat(couponRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldReturnNotFoundWhenCouponDoesNotExist() throws Exception {
        //when
        String c = createUseCouponRequest("spring", "appUser");

        //then
        mockMvc.perform(post("/coupon/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c))
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

        String c = createUseCouponRequest("spring", "appUser");

        //then
        mockMvc.perform(post("/coupon/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c))
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

        String c = createUseCouponRequest("spring", "invalid-user-id!");

        //then
        mockMvc.perform(post("/coupon/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSuccessfullyUseCoupon() throws Exception {
        //when
        couponRepository.saveAndFlush(new CouponEntity(
                "spring",
                Instant.now(),
                10,
                0,
                "PL"
        ));

        String c = createUseCouponRequest("spring", "appUser");

        //then
        mockMvc.perform(post("/coupon/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c))
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

        String c = createUseCouponRequest("spring", "appUser");

        //then
        mockMvc.perform(post("/coupon/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(c))
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

        String c = createUseCouponRequest("spring", "appUser");

        int requests = 2;

        //then
        List<String> responses = IntStream.range(0, requests)
                .parallel()
                .mapToObj(i -> {
                    try {
                        return mockMvc.perform(post("/coupon/use")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(c))
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
                        return mockMvc.perform(post("/coupon/use")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(generateUseCouponRequestWithRandomUserId("spring")))
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

    private String generateUseCouponRequestWithRandomUserId(String couponId) {
        return createUseCouponRequest(couponId, randomString(16));
    }

    private String createUseCouponRequest(String couponId, String userId) {
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
}