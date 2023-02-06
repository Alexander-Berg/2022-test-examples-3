package ru.yandex.market.ff.service.returns;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.enums.ReturnReasonType;
import ru.yandex.market.ff.model.returns.ReturnItemDto;
import ru.yandex.market.ff.model.returns.ReturnUnitComplexKey;
import ru.yandex.market.ff.repository.RegistryRepository;

class LrmAsyncReturnItemsFetchingServiceTest extends IntegrationTest {

    @Autowired
    private LrmAsyncReturnItemsFetchingService service;

    @Autowired
    private RegistryRepository registryRepository;

    @Test
    @DatabaseSetup("classpath:service/returns/lrm-items-fetching/before.xml")
    public void registryUnitWithSomeEqualReasonButDifferentUnitIds() {
        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey =
                service.getReturnItemsGroupedByKey(registry).getItemsData();

        List<ReturnItemDto> expectedResult = expectedResult();

        List<ReturnItemDto> actualResult = returnItemsGroupedByKey.entrySet()
                .stream()
                .flatMap(kv -> kv.getValue().stream())
                .collect(Collectors.toList());

        assertions.assertThat(actualResult).isEqualTo(expectedResult);
    }

    private List<ReturnItemDto> expectedResult() {
        RegistryUnitId registryUnitId1 = RegistryUnitId.of(
                RegistryUnitIdType.CIS,
                "2489571_item1_cis1",
                RegistryUnitIdType.UIT,
                "2489571_item1_uit1");
        ReturnItemDto returnItemDto1 = new ReturnItemDto(
                List.of(registryUnitId1),
                ReturnReasonType.DAMAGE_DELIVERY,
                "damaged",
                1);

        RegistryUnitId registryUnitId2 = RegistryUnitId.of(
                RegistryUnitIdType.CIS,
                "2489571_item2_cis2",
                RegistryUnitIdType.UIT,
                "2489571_item2_uit2");
        ReturnItemDto returnItemDto2 = new ReturnItemDto(
                List.of(registryUnitId2),
                ReturnReasonType.DO_NOT_FIT,
                "another",
                2);

        RegistryUnitId registryUnitId3 = RegistryUnitId.of(
                RegistryUnitIdType.CIS,
                "2489571_item1_cis1",
                RegistryUnitIdType.UIT,
                "2489571_item1_uit1");
        ReturnItemDto returnItemDto3 = new ReturnItemDto(
                List.of(registryUnitId3),
                ReturnReasonType.DAMAGE_DELIVERY,
                "damaged",
                1);

        return List.of(returnItemDto1, returnItemDto2, returnItemDto3);
    }
}
