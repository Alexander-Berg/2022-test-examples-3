package ru.yandex.market.core.orginfo.model;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.orginfo.model.OrganizationInfoSource.PARTNER_INTERFACE;
import static ru.yandex.market.core.orginfo.model.OrganizationInfoSource.YANDEX_MARKET;
import static ru.yandex.market.core.orginfo.model.OrganizationInfoSource.YA_MONEY;

/**
 * Юнит-тесты на {@link VersionedOrganizationInfo}.
 */
public class VersionedOrganizationInfoTest {

    @DisplayName("Проверка сортировки юр. инфо")
    @ParameterizedTest(name = "{2}")
    @MethodSource("testSortingPIOrgInfoData")
    public void testSortingPIOrgInfo(List<OrganizationInfo> data,
                                     List<OrganizationInfo> expectedOrder,
                                     String displayName) {
        List<OrganizationInfo> sortedData = data.stream()
                .sorted()
                .collect(Collectors.toList());
        assertThat(sortedData).containsExactlyElementsOf(expectedOrder);

        OrganizationInfo lastVersion = data.stream().max(Comparator.naturalOrder()).orElseThrow();
        assertThat(lastVersion).isEqualTo(sortedData.get(expectedOrder.size() - 1));
    }

    private static Stream<Arguments> testSortingPIOrgInfoData() {
        VersionedOrganizationInfo orgInfoPartner1Group1 = createVersionedOrgInfo(1, 1, PARTNER_INTERFACE);
        VersionedOrganizationInfo orgInfoPartner5Group1 = createVersionedOrgInfo(5, 1, PARTNER_INTERFACE);
        VersionedOrganizationInfo orgInfoPartner3Group2 = createVersionedOrgInfo(3, 2, PARTNER_INTERFACE);
        VersionedOrganizationInfo orgInfoMoney2Group2 = createVersionedOrgInfo(2, 2, YA_MONEY);
        VersionedOrganizationInfo orgInfoMarket3Group3 = createVersionedOrgInfo(3, 3, YANDEX_MARKET);

        return Stream.of(
                Arguments.of(
                        List.of(orgInfoPartner1Group1, orgInfoPartner5Group1),
                        List.of(orgInfoPartner1Group1, orgInfoPartner5Group1),
                        "Более свежая версия (с большим infoId) юр.инфо должна должна быть последней"),
                Arguments.of(
                        List.of(orgInfoPartner5Group1, orgInfoPartner3Group2),
                        List.of(orgInfoPartner3Group2, orgInfoPartner5Group1),
                        "Более свежая версия (с большим infoId) юр.инфо должна быть последней, несмотря на infoGroupId"),
                Arguments.of(
                        List.of(orgInfoMoney2Group2, orgInfoPartner5Group1),
                        List.of(orgInfoMoney2Group2, orgInfoPartner5Group1),
                        "Более свежая версия (с большим infoId) юр.инфо должна быть последней, несмотря на infoSource"),
                Arguments.of(
                        List.of(orgInfoMarket3Group3, orgInfoMoney2Group2, orgInfoPartner5Group1),
                        List.of(orgInfoMoney2Group2, orgInfoMarket3Group3, orgInfoPartner5Group1),
                        "Более свежая версия (с большим infoId) юр.инфо должна быть последней, микс из разных infoSource")
        );
    }

    private static VersionedOrganizationInfo createVersionedOrgInfo(long infoId,
                                                                    long infoGroupId,
                                                                    OrganizationInfoSource src) {
        VersionedOrganizationInfo orgInfo = new VersionedOrganizationInfo();
        orgInfo.setId(infoId);
        orgInfo.setInfoGroupId(infoGroupId);
        orgInfo.setInfoSource(src);
        return orgInfo;
    }
}
