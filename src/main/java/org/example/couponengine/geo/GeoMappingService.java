package org.example.couponengine.geo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxmind.geoip2.DatabaseReader;
import org.example.couponengine.commons.GeoLookupException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.time.Duration;

@Service
public class GeoMappingService {

    private final DatabaseReader reader;

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofHours(24))
            .build();

    public GeoMappingService() throws IOException {
        Resource resource = new ClassPathResource("geo/GeoLite2-Country.mmdb");
        try (InputStream is = resource.getInputStream()) {
            this.reader = new DatabaseReader.Builder(is).build();
        }
    }

    public String getCountryCode(String ip) {
        return cache.get(ip, this::lookup);
    }

    private String lookup(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return reader.country(inetAddress)
                    .country()
                    .isoCode();
        } catch (IllegalArgumentException e) {
            throw new GeoLookupException("Invalid IP format: " + ip);

        } catch (IOException e) {
            throw new GeoLookupException("GeoIP database error for IP: " + ip);

        } catch (Exception e) {
            throw new GeoLookupException("Unexpected GeoIP lookup failure for IP: " + ip);
        }
    }
}

