package ru.yandex.market.tpl.core.external.sms;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
class SmsWhitelistPhoneFilterTest {

    private static final String PHONE = "+79000000000";

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    private SmsWhitelistPhoneFilter smsWhitelistPhoneFilterUnderTest;

    @BeforeEach
    void init() {
        smsWhitelistPhoneFilterUnderTest = new SmsWhitelistPhoneFilter(configurationProviderAdapter);
    }

    private static List<Arguments> whitelistPassTestArgs() {
        return List.of(
                //when whitelist is enabled and phone is in whitelist, then passes
                Arguments.of(Optional.of("    ,+79000000000, +79000000000   , fsdfs   ,,+78000000000"), true),
                //when whitelist is enabled and phone is not in whitelist, then doesn't pass
                Arguments.of(Optional.of("   , fsdfs   ,,+78000000000"), false),
                //when whitelist is disabled, then doesn't pass
                Arguments.of(Optional.empty(), false)
        );
    }

    @ParameterizedTest
    @MethodSource("whitelistPassTestArgs")
    void whitelistPassTest(Optional<String> whitelist,
                           boolean expectedPassCheckResult) {
        given(configurationProviderAdapter.getValue(ConfigurationProperties.SMS_PHONE_WHITELIST))
                .willReturn(whitelist);
        //when
        boolean passes = smsWhitelistPhoneFilterUnderTest.passes(PHONE);
        //then
        assertThat(passes).isEqualTo(expectedPassCheckResult);
    }
}
