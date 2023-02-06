package ru.yandex.market.fintech.banksint.mybatis.installment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.InstallmentsResourceInfo;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.ResourceStatus;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.ResourceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallmentResourceMapperTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentResourceMapper installmentResourceMapper;

    private static Stream<Arguments> getInstallmentsResourceInfosByStatusesAndTypeTestData() {
        return Stream.of(
                Arguments.of(
                        Set.of(ResourceStatus.PENDING),
                        ResourceType.INSTALLMENT,
                        List.of("de7cd822-d5f9-47cb-8fee-36546d7c9ab1")
                ),
                Arguments.of(
                        Set.of(ResourceStatus.PENDING, ResourceStatus.DONE),
                        ResourceType.INSTALLMENT,
                        List.of("de7cd822-d5f9-47cb-8fee-36546d7c9ab1", "319021fd-a7d7-45d0-837d-82a8e4c4bce0",
                                "60ffd4cc-c84a-43a1-b290-1a9eee36e124")
                ),
                Arguments.of(
                        Set.of(ResourceStatus.PENDING, ResourceStatus.PROCESSING,
                                ResourceStatus.DONE, ResourceStatus.FAILED),
                        ResourceType.INSTALLMENT,
                        List.of("de7cd822-d5f9-47cb-8fee-36546d7c9ab1", "319021fd-a7d7-45d0-837d-82a8e4c4bce0",
                                "60ffd4cc-c84a-43a1-b290-1a9eee36e124", "85b438fc-a482-4fd7-83d1-505c6b5dcc30",
                                "cb929885-0805-41c4-b681-62b74d6ff90a")
                ),
                Arguments.of(
                        Set.of(ResourceStatus.PENDING, ResourceStatus.PROCESSING,
                                ResourceStatus.DONE, ResourceStatus.FAILED),
                        ResourceType.TEMPLATE,
                        List.of("691634f8-3ab9-4549-a7b8-53147335eff9", "c02c6b8c-936d-4373-8a7d-bd8bbe8aa72d")
                ),
                Arguments.of(
                        Set.of(ResourceStatus.PENDING),
                        ResourceType.TEMPLATE,
                        List.of("691634f8-3ab9-4549-a7b8-53147335eff9")
                ),
                Arguments.of(
                        Set.of(ResourceStatus.DONE, ResourceStatus.FAILED),
                        ResourceType.TEMPLATE,
                        List.of()
                )
        );
    }

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentResourceMapper.sql"));
    }

    @ParameterizedTest
    @MethodSource("getInstallmentsResourceInfosByStatusesAndTypeTestData")
    void getInstallmentsResourceInfosByStatusesAndTypeShouldWorkProperly(
            Set<ResourceStatus> statuses,
            ResourceType type,
            List<String> expectedResourceIds
    ) {
        List<InstallmentsResourceInfo> resources =
                installmentResourceMapper.getInstallmentsResourceInfosByStatusesAndType(statuses, type);
        List<InstallmentsResourceInfo> expectedResources =
                expectedResourceIds.stream().map(installmentResourceMapper::getInstallmentsResourceInfoByResourceId)
                        .collect(Collectors.toList());

        assertThat(resources).hasSameElementsAs(expectedResources);
    }

    @Test
    void updateInstallmentsResourceShouldWorkProperly() {
        InstallmentsResourceInfo resource =
                installmentResourceMapper.getInstallmentsResourceInfoByResourceIdAndBusinessIdAndShopId(
                        "de7cd822-d5f9-47cb-8fee-36546d7c9ab1", 2L, 42L);

        resource.setName("updated_filename.xlsx");
        resource.setTotalOffers(100L);
        resource.setCorrectSelectedOffers(25L);
        resource.setInvalidOffers(75L);
        resource.setUrlToDownload("new-url-to-download");
        resource.setStatus(ResourceStatus.FAILED);
        resource.setFailReason("неверный формат файла");
        installmentResourceMapper.updateInstallmentsResource(resource);

        InstallmentsResourceInfo updatedResource =
                installmentResourceMapper.getInstallmentsResourceInfoByResourceIdAndBusinessIdAndShopId(
                        "de7cd822-d5f9-47cb-8fee-36546d7c9ab1", 2L, 42L);

        assertThat(updatedResource)
                .usingRecursiveComparison()
                .ignoringFields("updatedAt")
                .isEqualTo(resource);
    }

    @Test
    void updateInstallmentsResourceStatusShouldWorkProperly() {
        installmentResourceMapper.updateInstallmentsResourceStatus(
                "de7cd822-d5f9-47cb-8fee-36546d7c9ab1", 2L, 42L, ResourceStatus.DONE);

        InstallmentsResourceInfo updatedResource =
                installmentResourceMapper.getInstallmentsResourceInfoByResourceIdAndBusinessIdAndShopId(
                        "de7cd822-d5f9-47cb-8fee-36546d7c9ab1", 2L, 42L);

        assertEquals(ResourceStatus.DONE, updatedResource.getStatus());
    }

    @Test
    void findInstallmentResourceIdsForValidationResubmissionShouldWorkProperly() {
        jdbcTemplate.execute(readClasspathFile("InstallmentResourceMapper_2.sql"));

        List<String> resourceIds =
                installmentResourceMapper.findInstallmentResourceIdsForValidationResubmission(
                "1 minute", "5 minutes", "1 hour");
        assertThat(resourceIds).hasSameElementsAs(List.of("de7cd822-d5f9-47cb-8fee-36546d7c9ab1",
                "85b438fc-a482-4fd7-83d1-505c6b5dcc30"));
    }
}
