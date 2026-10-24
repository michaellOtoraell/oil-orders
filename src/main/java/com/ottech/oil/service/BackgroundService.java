package com.ottech.oil.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Inasoma picha zilizo kwenye src/main/resources/static/img/bg/
@Service
public class BackgroundService {

    private static final Set<String> EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private volatile List<String> cached;

    public List<String> list() {
        if (cached == null) {
            cached = scan();
        }
        return cached;
    }

    private List<String> scan() {
        try {
            Resource[] found = resolver.getResources("classpath:/static/img/bg/*");
            return Arrays.stream(found)
                    .map(Resource::getFilename)
                    .filter(Objects::nonNull)
                    .filter(this::isImage)
                    .sorted()
                    .map(n -> "/img/bg/" + UriUtils.encodePathSegment(n, StandardCharsets.UTF_8))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private boolean isImage(String name) {
        String lower = name.toLowerCase();
        return EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
