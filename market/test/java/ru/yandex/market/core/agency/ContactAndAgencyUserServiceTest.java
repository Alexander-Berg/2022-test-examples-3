package ru.yandex.market.core.agency;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "ContactAndAgencyUserServiceTest.before.csv")
class ContactAndAgencyUserServiceTest extends FunctionalTest {

    @Autowired
    private ContactAndAgencyUserService contactAndAgencyUserService;

    public static Stream<Arguments> dataGetLinkedCampaignIdsByUid() {
        return Stream.of(
                Arguments.of("uid агентский - кампании через клиента", 1001L, List.of(100101L, 100102L)),
                Arguments.of("uid пользовательский, через линку к кампании", 1002L, List.of(100201L)),
                Arguments.of("uid пользовательский, через линку к бизнесу", 1003L, List.of(100301L, 100302L, 100303L)),
                Arguments.of("uid пользовательский, без линок", 1004L, List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("dataGetLinkedCampaignIdsByUid")
    void getLinkedCampaignIdsByUid(String description, long uid, List<Long> expectedCampaigns) {
        assertThat(contactAndAgencyUserService.getLinkedCampaignIdsByUid(uid))
                .containsExactlyInAnyOrderElementsOf(expectedCampaigns);
    }
}
