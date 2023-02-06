package ru.yandex.market.ff.controller.test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.xml.bind.annotation.XmlElement;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.registry.RegistryEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;
import ru.yandex.market.ff.model.returns.ReturnItemFromCheckouterYtDto;
import ru.yandex.market.ff.model.returns.ReturnUnitFromCheckouterYtComplexKey;
import ru.yandex.market.ff.service.returns.RussianPostalServiceAsyncReturnItemsFetchingService;
import ru.yandex.market.ff.service.yt.YtJdbcService;

@Api("Контроллер для различных тестовых проверок")
@RestController
@RequiredArgsConstructor
public class TestController {

    private final YtJdbcService ytJdbcService;
    private final RussianPostalServiceAsyncReturnItemsFetchingService
            russianPostalServiceAsyncReturnItemsFetchingService;

    @PostMapping("/test/checkouter-yt-for-returns")
    public String checkouterYtForReturns(@RequestBody CheckouterYtForReturnsDto request) {
        Map<ReturnUnitFromCheckouterYtComplexKey, List<ReturnItemFromCheckouterYtDto>> result = ytJdbcService
                .selectCheckouterReturnsDataFromYt(new HashSet<>(request.getBoxIds()), request.getDeliveryServiceId());
        return result.toString();
    }

    @PostMapping("/test/russian-post-returns-returns-enrichment")
    public String russianPostReturnsEnrichment(@RequestBody CheckouterYtForReturnsDto request) {
        RegistryEntity registry = new RegistryEntity();

        List<RegistryUnitEntity> registryUnits = request.getBoxIds()
                .stream()
                .map(boxId -> {
                    RegistryUnitEntity registryUnit = new RegistryUnitEntity();
                    registryUnit.setType(RegistryUnitType.BOX);
                    RegistryUnitId registryUnitId =
                            new RegistryUnitId(Set.of(new UnitPartialId(RegistryUnitIdType.BOX_ID, boxId)));
                    registryUnit.setIdentifiers(registryUnitId);
                    return registryUnit;
                })
                .collect(Collectors.toList());
        registry.setRegistryUnits(registryUnits);

        return russianPostalServiceAsyncReturnItemsFetchingService.getReturnItemsGroupedByKey(registry).toString();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class CheckouterYtForReturnsDto {
        @NotEmpty
        @XmlElement(name = "boxIds")
        private List<String> boxIds;
        @Positive
        @XmlElement(name = "deliveryServiceId")
        private long deliveryServiceId;
    }
}
