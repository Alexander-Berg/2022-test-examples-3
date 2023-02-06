package ru.yandex.market.sec;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cocon.CabinetService;
import ru.yandex.market.cocon.model.AuthorityKey;
import ru.yandex.market.cocon.model.BaseSecurable;
import ru.yandex.market.cocon.model.Cabinet;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.cocon.model.SecurityRule;
import ru.yandex.market.cocon.reader.AuthorityParser;
import ru.yandex.market.security.model.Authority;

@Disabled
class CabinetAuthoritiesTest extends JavaSecFunctionalTest {

    @Autowired
    private CabinetService cabinetService;

    static Stream<Arguments> cabinetTypes() {
        return Stream.of(CabinetType.values()).map(Arguments::of);
    }

    /**
     * Проверка, что в описаниях всех кабинетов используются только существующие роли.
     */
    @DisplayName("В описании кабинетов нет несуществующих ролей")
    @ParameterizedTest
    @MethodSource("cabinetTypes")
    void cabinetIsGood(CabinetType cabinetType) {
        Cabinet cabinet = cabinetService.getCabinet(cabinetType);
        Set<AuthorityKey> authorities = collectAuthorities(cabinet).stream()
                .distinct()
                .map(AuthorityParser::parse)
                .collect(Collectors.toSet());

        for (AuthorityKey authKey : authorities) {
            Authority auth = authoritiesLoader.load(DOMAIN, authKey.getAuthority(), authKey.getParam());
            Assertions.assertNotNull(auth, () -> String.format("Authority %s should be present", authKey));
        }
    }

    private Set<String> collectAuthorities(BaseSecurable securable) {
        Set<String> authorities = new HashSet<>();
        securable.getStates().ifPresent(rule -> authorities.addAll(collectAuthorities(rule)));
        securable.getRoles().ifPresent(rule -> authorities.addAll(collectAuthorities(rule)));
        if (securable.getChildren() != null) {
            for (BaseSecurable child : securable.getChildren()) {
                authorities.addAll(collectAuthorities(child));

            }
        }
        return authorities;
    }

    private Set<String> collectAuthorities(SecurityRule securityRule) {
        return securityRule != null && securityRule.getItems() != null
                ? new HashSet<>(securityRule.getItems())
                : Collections.emptySet();
    }
}

