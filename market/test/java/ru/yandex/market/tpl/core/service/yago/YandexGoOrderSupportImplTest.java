package ru.yandex.market.tpl.core.service.yago;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.routing.tag.OptionalRoutingTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderProperties;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderSupportImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YandexGoOrderSupportImplTest {

    @InjectMocks
    YandexGoOrderSupportImpl yandexGoSupport;

    @Mock
    YandexGoOrderProperties props;

    @Mock
    UserPropertyService userPropertyService;

    @Test
    void shouldNotMatchUser_WhenMatchUser_IfConfigurationPropertyIsNull() {
        // given
        Company company = Company.builder().id(321L).build();
        User user = UserUtil.withCompany(company);
        when(props.getCompanyIds()).thenReturn(Set.of());

        // when
        boolean matched = yandexGoSupport.matchesUser(user);

        // then
        assertThat(matched).isFalse();
    }

    @Test
    void shouldNotMatchUser_WhenMatchUser_IfConfigurationPropertyDoesNotHaveUserCompanyId() {
        // given
        Company company = Company.builder().id(321L).build();
        User user = UserUtil.withCompany(company);
        when(props.getCompanyIds()).thenReturn(Set.of(999L));

        // when
        boolean matched = yandexGoSupport.matchesUser(user);

        // then
        assertThat(matched).isFalse();
    }

    @ParameterizedTest
    @MethodSource("getYandexGoCompaniesIdCases")
    void shouldNotMatchUser_WhenMatchUser_IfConfigurationPropertyContainsUserCompanyId(List<Long> companyIds) {
        // given
        Company company = Company.builder().id(321L).build();
        User user = UserUtil.withCompany(company);
        when(props.getCompanyIds()).thenReturn(new HashSet<>(companyIds));

        // when
        boolean matched = yandexGoSupport.matchesUser(user);

        // then
        assertThat(matched).isTrue();
    }

    private static Stream<Arguments> getYandexGoCompaniesIdCases() {
        return Stream.of(
                Arguments.of(List.of(321L)),
                Arguments.of(List.of(321L, 1L)),
                Arguments.of(List.of(1L, 321L, 2L))
        );
    }

    @Test
    void shouldAddExpectedTags_whenEnsureUserTags() {
        // given
        // when
        User user = new User();
        when(userPropertyService.findPropertyForUser(UserProperties.ROUTING_TAGS, user)).thenReturn(
                Set.of(RequiredRoutingTag.PREPAID.getCode(), RequiredRoutingTag.DELIVERY.getCode(),
                        OptionalRoutingTag.CLIENT.getCode())
        );
        yandexGoSupport.ensureUserTags(user);

        // then
        assertThat(userPropertyService.findPropertyForUser(UserProperties.ROUTING_TAGS, user))
                .contains(
                        RequiredRoutingTag.PREPAID.getCode(),
                        RequiredRoutingTag.DELIVERY.getCode(),
                        OptionalRoutingTag.CLIENT.getCode());
    }

}
