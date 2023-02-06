package ru.yandex.market.core.application.business;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.business.model.BusinessApplicationInfo;
import ru.yandex.market.core.application.business.repository.BusinessApplicationFilter;
import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Тесты для {@link BusinessApplicationService}.
 */
@DbUnitDataSet(before = "BusinessApplicationServiceTest.csv")
public class BusinessApplicationServiceTest extends FunctionalTest {
    private static final BusinessApplicationInfo request2 = BusinessApplicationInfo.builder()
            .withRequestId(2)
            .withSellerClientId(502L)
            .withContractId(602L)
            .withContractEid("469743/20")
            .withJurName("orgName2")
            .withOrganizationType(OrganizationType.ZAO)
            .withPartnerIds(Set.of(200L, 300L))
            .build();
    private static final BusinessApplicationInfo request3 = BusinessApplicationInfo.builder()
            .withRequestId(3)
            .withSellerClientId(503L)
            .withContractId(603L)
            .withContractEid("235253/20")
            .withJurName("orgName3")
            .withOrganizationType(OrganizationType.OOO)
            .withPartnerIds(Set.of(200L))
            .build();
    private static final BusinessApplicationInfo request4 = BusinessApplicationInfo.builder()
            .withRequestId(4)
            .withContractEid("")
            .withJurName("orgName4")
            .withOrganizationType(OrganizationType.IP)
            .withPartnerIds(Set.of(400L))
            .build();
    private static final BusinessApplicationInfo request5 = BusinessApplicationInfo.builder()
            .withRequestId(5)
            .withSellerClientId(505L)
            .withContractId(605L)
            .withContractEid("565744/20")
            .withJurName("orgName5")
            .withOrganizationType(OrganizationType.AO)
            .withPartnerIds(Set.of(100L))
            .build();
    private static final BusinessApplicationInfo request2_for_admin = BusinessApplicationInfo.builder()
            .withRequestId(2)
            .withSellerClientId(502L)
            .withContractId(602L)
            .withContractEid("469743/20")
            .withJurName("orgName2")
            .withOrganizationType(OrganizationType.ZAO)
            .withPartnerIds(Set.of(200L))
            .build();

    static Stream<Arguments> findBusinessApplicationsData() {
        return Stream.of(
                Arguments.of("Все договоры в бизнесе 2000",
                        BusinessApplicationFilter.builder().withBusinessId(2000L).build(),
                        List.of(request2, request3, request4)),
                Arguments.of("Договоры в бизнесе 2000 с контрактом",
                        BusinessApplicationFilter.builder().withBusinessId(2000L).withContract(true).build(),
                        List.of(request2, request3)),
                Arguments.of("Договоры в бизнесе 2000 в статусе COMPLETED",
                        BusinessApplicationFilter.builder().withBusinessId(2000L).onlyCompleted(true).build(),
                        List.of(request2, request4)),
                Arguments.of("Договоры в бизнесе 2000 владельца бизнеса",
                        BusinessApplicationFilter.builder().withBusinessId(2000L).withContactId(1L).build(),
                        List.of(request2, request3, request4)),
                Arguments.of("Договоры в бизнесе 2000 админа 1 из магазинов",
                        BusinessApplicationFilter.builder().withBusinessId(2000L).withContactId(2L).build(),
                        List.of(request2_for_admin, request3)),
                Arguments.of("Договоры в бизнесе 1000 со всеми условиями",
                        BusinessApplicationFilter.builder().withBusinessId(1000L)
                                .withContactId(1L)
                                .withContract(true)
                                .onlyCompleted(true).build(),
                        List.of(request5)),
                Arguments.of("Договоры без условий фильтрации",
                        BusinessApplicationFilter.builder().build(),
                        List.of(request2,request3, request4, request5)),
                Arguments.of("Договоры c активными кампаниями",
                        BusinessApplicationFilter.builder().withActiveCampaign(true).build(),
                        List.of(request2_for_admin, request3, request5))
        );
    }

    @Autowired
    private BusinessApplicationService businessApplicationService;

    @ParameterizedTest(name = "{0}")
    @MethodSource("findBusinessApplicationsData")
    void findBusinessApplicationsTest(String testName, BusinessApplicationFilter filter,
                                      List<BusinessApplicationInfo> expected) {
        List<BusinessApplicationInfo> actual = businessApplicationService.findBusinessApplications(filter);
        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }
}
