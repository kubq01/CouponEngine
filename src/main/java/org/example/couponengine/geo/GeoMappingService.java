package org.example.couponengine.geo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxmind.geoip2.DatabaseReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
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
        File db = new ClassPathResource("geo/GeoLite2-Country.mmdb").getFile();
        this.reader = new DatabaseReader.Builder(db).build();
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
        } catch (Exception e) {
            throw new RuntimeException("GeoIP lookup failed", e);
        }
    }
}

