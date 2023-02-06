package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.sspplatform.container.SspInfo;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;

@Component
@ParametersAreNonnullByDefault
public class SspPlatformsSteps {
    private final SspPlatformsRepository sspPlatformsRepository;

    public SspPlatformsSteps(SspPlatformsRepository sspPlatformsRepository) {
        this.sspPlatformsRepository = sspPlatformsRepository;
    }

    /**
     * Добавить в таблицу ppcdict.ssp_platforms названия новых SSP-площадок.
     * Площадки, которые уже есть в таблице, отбрасываются
     */
    public void addSspPlatforms(List<String> sspPlatformsTitles) {
        List<SspInfo> sspInfoList = sspPlatformsTitles.stream()
                .map(it -> new SspInfo(it, 0L, true))
                .collect(Collectors.toList());
        sspPlatformsRepository.mergeSspPlatforms(sspInfoList);
    }
}
