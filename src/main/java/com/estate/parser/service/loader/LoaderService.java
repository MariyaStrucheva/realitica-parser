package com.estate.parser.service.loader;

import com.estate.parser.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoaderService {
    private final List<IContentLoader> contentLoaders;
    private final AdRepository adRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "0 0 19 * * *") // every day at 19:00
    public void load() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Loader is already running, skip");
            return;
        }
        try {
            contentLoaders.parallelStream().forEach(loader -> {
                try {
                    log.info("Start loading from {}", loader.getSourceName());
                    var ids = loader.loadAndSave();
                    log.info("Finish loading from {}, count {}", loader.getSourceName(), ids.size());
                } catch (Exception e) {
                    log.error("Error loading from {}", loader.getSourceName(), e);
                }
            });
        } finally {
            running.set(false);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Scheduled(cron = "0 0 18 * * SUN") // every Sunday at 18:00
    private void removeDeprecated() {
        try {
            log.info("Start scheduler to remove deprecated");
            var deprecatedDate = OffsetDateTime.now().minusMonths(2);
            var toRemoveDate = LocalDateTime.now().minusMonths(12);
            var deprecatedAdEntities = adRepository.findAll().stream()
                    .filter(s -> s.getUpdated() == null || s.getUpdated().isBefore(deprecatedDate))
                    .filter(s -> {
                        if (s.getLastModified() != null && s.getLastModified().isBefore(toRemoveDate)) {
                            log.info("Stun {} is deprecated", s.getId());
                            return true;
                        }
                        return resolveLoader(s.getSourceCode()).isCanBeDeleted(s.getSourceId());
                    })
                    .collect(Collectors.toList());
            log.info("Found entities to remove {}", deprecatedAdEntities.size());
            adRepository.deleteAll(deprecatedAdEntities);
            log.info("Finish removing");
        } catch (Exception e) {
            log.error("Error removing deprecated ads", e);
        }
    }

    private IContentLoader resolveLoader(String sourceCode) {
        return contentLoaders.stream()
                .filter(l -> l.getSourceName().equals(sourceCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Loader not found for " + sourceCode));
    }

}
