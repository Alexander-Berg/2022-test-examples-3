package ru.yandex.market.vendors.analytics.platform.regent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.vendors.analytics.core.regent.dto.RegentRequestDto;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.regent.facade.RegentFacade;

import java.util.Map;

public abstract class AbstractRegentTest extends FunctionalTest {
    @Autowired
    protected RegentFacade regentFacade;

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    public RegentRequestDto makeRequest(String preset, Map<String, Object> parameters, boolean columns){
        var dto = new RegentRequestDto();
        dto.setPresetName(preset);
        dto.setSortSpec(null);
        dto.setPaging(null);
        dto.setDistinct(null);
        dto.setParameters(parameters);
        dto.setResultAsColumns(columns);
        dto.setExport(false);
        return dto;
    }
}
