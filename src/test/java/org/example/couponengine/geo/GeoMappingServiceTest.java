package org.example.couponengine.geo;

import org.example.couponengine.commons.GeoLookupException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoMappingServiceTest {

    @Test
    void shouldReturnCountryCodeForPublicIp() throws Exception {
        GeoMappingService service = new GeoMappingService();

        String result = service.getCountryCode("8.8.8.8");

        assertNotNull(result);
        assertEquals(2, result.length());
        assertTrue(result.matches("[A-Z]{2}"));
    }

    @Test
    void shouldThrowForInvalidIpFormat() throws Exception {
        GeoMappingService service = new GeoMappingService();

        assertThrows(GeoLookupException.class,
                () -> service.getCountryCode("not-an-ip"));
    }

    @Test
    void shouldThrowForNullIp() throws Exception {
        GeoMappingService service = new GeoMappingService();

        assertThrows(NullPointerException.class,
                () -> service.getCountryCode(null));
    }

    @Test
    void shouldThrowForEmptyIp() throws Exception {
        GeoMappingService service = new GeoMappingService();

        assertThrows(GeoLookupException.class,
                () -> service.getCountryCode(""));
    }

    @Test
    void shouldHandleUnresolvableIp() throws Exception {
        GeoMappingService service = new GeoMappingService();

        assertThrows(GeoLookupException.class,
                () -> service.getCountryCode("0.0.0.0"));
    }

    @Test
    void shouldSupportIpv6() throws Exception {
        GeoMappingService service = new GeoMappingService();

        String result = service.getCountryCode("2001:4860:4860::8888");

        assertNotNull(result);
        assertEquals(2, result.length());
        assertTrue(result.matches("[A-Z]{2}"));
    }
}