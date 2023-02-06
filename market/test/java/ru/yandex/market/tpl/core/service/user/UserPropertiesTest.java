package ru.yandex.market.tpl.core.service.user;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.base.property.BooleanDefinition;
import ru.yandex.market.tpl.core.domain.base.property.LongDefinition;
import ru.yandex.market.tpl.core.domain.base.property.PropertyDefinition;
import ru.yandex.market.tpl.core.domain.base.property.StringSetDefinition;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class UserPropertiesTest extends TplAbstractTest {

    private final UserPropertyService userPropertyService;
    private final UserRepository userRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final TestUserHelper userHelper;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private StringSetDefinition stringSetProperty;
    private BooleanDefinition booleanProperty;
    private LongDefinition longProperty;


    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        user = userRepository.save(user);
        stringSetProperty = new StringSetDefinition("string-set-prop", Set.of("value-1"));
        booleanProperty = new BooleanDefinition("bool-prop", true);
        longProperty = new LongDefinition("long-prop", 1L);
    }

    @AfterEach
    void afterEach() {
        userPropertyService.resetPropertyForUser(user, stringSetProperty);
        userPropertyService.resetPropertyForUser(user, booleanProperty);
        userPropertyService.resetPropertyForUser(user, longProperty);
    }

    @Test
    void getDefaultPropertyValueTest() {
        Set<String> actualSet = userPropertyService.findPropertyForUser(stringSetProperty, user);
        assertThat(actualSet).isEqualTo(Set.of("value-1"));

        boolean actualBooleanValue = userPropertyService.findPropertyForUser(booleanProperty, user);
        assertThat(actualBooleanValue).isTrue();

        long actualLongValue = userPropertyService.findPropertyForUser(longProperty, user);
        assertThat(actualLongValue).isEqualTo(1L);
    }

    @Test
    void findUserPropertyValueByServiceTest() {
        transactionTemplate.execute((t) -> {
            userPropertyService.addPropertyToUser(user, stringSetProperty, Set.of("another-value"));
            userPropertyService.addPropertyToUser(user, booleanProperty, false);
            userPropertyService.addPropertyToUser(user, longProperty, 42L);
            return null;
        });

        Set<String> actualSet = userPropertyService.findPropertyForUser(stringSetProperty, user);
        assertThat(actualSet).isEqualTo(Set.of("another-value"));

        boolean actualBooleanValue = userPropertyService.findPropertyForUser(booleanProperty, user);
        assertThat(actualBooleanValue).isFalse();

        long actualLongValue = userPropertyService.findPropertyForUser(longProperty, user);
        assertThat(actualLongValue).isEqualTo(42L);
    }

    @Test
    void findAllUserPropertiesTestWithoutScProperties() {
        PropertyDefinition<Boolean> callToRecipientEnabled = UserProperties.CALL_TO_RECIPIENT_ENABLED;
        PropertyDefinition<Boolean> hideClientPhoneNumberForCourier =
                UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER;
        PropertyDefinition<Boolean> demoEnabled = UserProperties.DEMO_ENABLED;
        PropertyDefinition<String> userMode = UserProperties.USER_MODE;
        PropertyDefinition<String> customVersionNumber =  UserProperties.CUSTOM_VERSION_NUMBER;

        transactionTemplate.execute((t) -> {
            userPropertyService.addPropertyToUser(user, callToRecipientEnabled, false);
            userPropertyService.addPropertyToUser(user, demoEnabled, false);
            userPropertyService.addPropertyToUser(user, customVersionNumber, "value");
            return null;
        });


        Map<PropertyDefinition<?>, ?> userProperties = userPropertyService.findAllPropertiesForUser(user);
        assertThat((Boolean) userProperties.get(callToRecipientEnabled)).isFalse();
        assertThat((Boolean) userProperties.get(hideClientPhoneNumberForCourier)).isNull();
        assertThat((Boolean) userProperties.get(demoEnabled)).isFalse();
        assertThat((String) userProperties.get(userMode)).isNull();
        assertThat((String) userProperties.get(customVersionNumber)).isEqualTo("value");
    }

    @Test
    void findAllUserPropertiesTestWithScProperties() {
        PropertyDefinition<Boolean> callToRecipientEnabled = UserProperties.CALL_TO_RECIPIENT_ENABLED;
        PropertyDefinition<Boolean> hideClientPhoneNumberForCourier =
                UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER;
        PropertyDefinition<Boolean> demoEnabled = UserProperties.DEMO_ENABLED;
        PropertyDefinition<String> userMode = UserProperties.USER_MODE;
        PropertyDefinition<String> customVersionNumber =  UserProperties.CUSTOM_VERSION_NUMBER;
        transactionTemplate.execute((t) -> {
            userPropertyService.addPropertyToUser(user, callToRecipientEnabled, false);
            userPropertyService.addPropertyToUser(user, demoEnabled, false);
            userPropertyService.addPropertyToUser(user, customVersionNumber, "value");
            return null;
        });

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);
        assertThat(userShift.isActive()).isTrue();
        assertThat(userShift.getShift().getSortingCenter().getId()).isEqualTo(SortingCenter.DEFAULT_SC_ID);

        Optional<SortingCenter> scOpt = sortingCenterRepository.findById(SortingCenter.DEFAULT_SC_ID);
        assertThat(scOpt).isPresent();
        SortingCenter sortingCenter = scOpt.get();

        SortingCenterPropertyEntity sortingCenterPropertyEntityFifth =
                sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter, customVersionNumber, "new");
        assertThat(sortingCenterPropertyEntityFifth).extracting(SortingCenterPropertyEntity::getId).isNotNull();

        SortingCenterPropertyEntity sortingCenterPropertyEntityFourth =
                sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter, userMode, "new");

        assertThat(sortingCenterPropertyEntityFourth).extracting(SortingCenterPropertyEntity::getId).isNotNull();

        Map<PropertyDefinition<?>, ?> userProperties = userPropertyService.findAllPropertiesForUser(user);
        assertThat((Boolean) userProperties.get(callToRecipientEnabled)).isFalse();
        assertThat((Boolean) userProperties.get(hideClientPhoneNumberForCourier)).isNull();
        assertThat((Boolean) userProperties.get(demoEnabled)).isFalse();
        assertThat((String) userProperties.get(userMode)).isEqualTo("new");
        assertThat((String) userProperties.get(customVersionNumber)).isEqualTo("value");
    }


    @Test
    void findAllUserPropertiesTestWithOutdatedScProperties() {
        PropertyDefinition<String> customVersionNumber =  UserProperties.CUSTOM_VERSION_NUMBER;

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);
        assertThat(userShift.isActive()).isTrue();
        assertThat(userShift.getShift().getSortingCenter().getId()).isEqualTo(SortingCenter.DEFAULT_SC_ID);

        Optional<SortingCenter> scOpt = sortingCenterRepository.findById(SortingCenter.DEFAULT_SC_ID);
        assertThat(scOpt).isPresent();
        SortingCenter sortingCenter = scOpt.get();

        SortingCenterPropertyEntity sortingCenterPropertyEntityFifth =
                sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter, customVersionNumber, "new",
                        Instant.now().minus(Duration.ofDays(4)), Instant.now().minus(Duration.ofDays(2)));
        assertThat(sortingCenterPropertyEntityFifth).extracting(SortingCenterPropertyEntity::getId).isNotNull();

        Map<PropertyDefinition<?>, ?> userProperties = userPropertyService.findAllPropertiesForUser(user);
        assertThat((String) userProperties.get(customVersionNumber)).isNull();
    }

    @Test
    void findUserPropertyValueByUserClassMethodInSingleTransactionTest() {
        transactionTemplate.execute((t) -> {
            userPropertyService.addPropertyToUser(user, stringSetProperty, Set.of("another-value"));
            return null;
        });
        Set<String> actualSet = user.getPropertyValue(stringSetProperty);
        assertThat(actualSet).isEqualTo(Set.of("another-value"));
    }

    @Test
    void findUserPropertyValueByUserClassMethodInIndependentTransactionTest() {
        transactionTemplate.execute((t) -> {
            userPropertyService.addPropertyToUser(user, stringSetProperty, Set.of("another-value"));
            return  null;
        });

        transactionTemplate.execute((t) -> {
            user = userRepository.findById(user.getId()).orElseThrow();
            Set<String> actualSet = user.getPropertyValue(stringSetProperty);
            assertThat(actualSet).isEqualTo(Set.of("another-value"));
            return  null;
        });
    }

    @Test
    void findSetStringPropertyWithBorderSpacesTest() {
        stringSetProperty = new StringSetDefinition("string-set-prop", Set.of("value-1", "value-2"));
        transactionTemplate.execute((t) -> {
            userPropertyService.addPropertyToUser(user, stringSetProperty, Set.of("  prop-value-1", " prop-value-2 "));
            return  null;
        });

        Set<String> actualSet = userPropertyService.findPropertyForUser(stringSetProperty, user);
        assertThat(actualSet).isEqualTo(Set.of("prop-value-1", "prop-value-2"));
    }

    @Test
    void findSetStringPropertyWithMoreThanOneValueTest() {
        stringSetProperty = new StringSetDefinition("string-set-prop",
                Set.of("value-1", "value-2"));

        Set<String> actualSet = userPropertyService.findPropertyForUser(stringSetProperty, user);
        assertThat(actualSet).isEqualTo(Set.of("value-1", "value-2"));
    }

    @Test
    void getCurrentUserShiftSortingCenterPropertyValueTest() {
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);
        assertThat(userShift.isActive()).isTrue();
        assertThat(userShift.getShift().getSortingCenter().getId()).isEqualTo(SortingCenter.DEFAULT_SC_ID);

        Optional<SortingCenter> scOpt = sortingCenterRepository.findById(SortingCenter.DEFAULT_SC_ID);
        assertThat(scOpt).isPresent();
        SortingCenter sortingCenter = scOpt.get();

        SortingCenterPropertyEntity sortingCenterPropertyEntity =
                sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter, longProperty, 3L);
        assertThat(sortingCenterPropertyEntity).extracting(SortingCenterPropertyEntity::getId).isNotNull();
        long actualLongValue = userPropertyService.findPropertyForUser(longProperty, user);
        assertThat(actualLongValue).isEqualTo(3L);
        sortingCenterPropertyService.deletePropertyFromSortingCenter(sortingCenter, longProperty);
    }

    @Test
    void getCurrentUserShiftSortingCenterPropertyValueWithNotNullActivationDatesTest() {
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);
        assertThat(userShift.isActive()).isTrue();
        assertThat(userShift.getShift().getSortingCenter().getId()).isEqualTo(SortingCenter.DEFAULT_SC_ID);

        Optional<SortingCenter> scOpt = sortingCenterRepository.findById(SortingCenter.DEFAULT_SC_ID);
        assertThat(scOpt).isPresent();
        SortingCenter sortingCenter = scOpt.get();

        SortingCenterPropertyEntity sortingCenterPropertyEntity =
                sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter, longProperty, 3L,
                        Instant.now().minus(Duration.ofDays(2)), Instant.now().plus(Duration.ofDays(2)));
        assertThat(sortingCenterPropertyEntity).extracting(SortingCenterPropertyEntity::getId).isNotNull();
        long actualLongValue = userPropertyService.findPropertyForUser(longProperty, user);
        assertThat(actualLongValue).isEqualTo(3L);
        sortingCenterPropertyService.deletePropertyFromSortingCenter(sortingCenter, longProperty);
    }

    @Test
    void getCurrentUserShiftSortingCenterPropertyValueWithNotNullActivationDatesInPastTest() {
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);
        assertThat(userShift.isActive()).isTrue();
        assertThat(userShift.getShift().getSortingCenter().getId()).isEqualTo(SortingCenter.DEFAULT_SC_ID);

        Optional<SortingCenter> scOpt = sortingCenterRepository.findById(SortingCenter.DEFAULT_SC_ID);
        assertThat(scOpt).isPresent();
        SortingCenter sortingCenter = scOpt.get();

        SortingCenterPropertyEntity sortingCenterPropertyEntity =
                sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter, longProperty, 3L,
                        Instant.now().minus(Duration.ofDays(4)), Instant.now().minus(Duration.ofDays(2)));
        assertThat(sortingCenterPropertyEntity).extracting(SortingCenterPropertyEntity::getId).isNotNull();
        long actualLongValue = userPropertyService.findPropertyForUser(longProperty, user);
        assertThat(actualLongValue).isEqualTo(1L);
        sortingCenterPropertyService.deletePropertyFromSortingCenter(sortingCenter, longProperty);
    }

    @Test
    @Transactional
    void userPropertyOverrideSortingCenterPropertyTest() {
        transactionTemplate.execute((t) -> {
            userPropertyService.addPropertyToUser(user, longProperty, 2L);
            return  null;
        });

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);
        assertThat(userShift.isActive()).isTrue();
        assertThat(userShift.getShift().getSortingCenter().getId()).isEqualTo(SortingCenter.DEFAULT_SC_ID);

        Optional<SortingCenter> scOpt = sortingCenterRepository.findById(SortingCenter.DEFAULT_SC_ID);
        assertThat(scOpt).isPresent();
        SortingCenter sortingCenter = scOpt.get();

        SortingCenterPropertyEntity sortingCenterPropertyEntity =
                sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter, longProperty, 3L);
        assertThat(sortingCenterPropertyEntity).extracting(SortingCenterPropertyEntity::getId).isNotNull();
        long actualLongValue = userPropertyService.findPropertyForUser(longProperty, user);
        assertThat(actualLongValue).isEqualTo(2L);
        sortingCenterPropertyService.deletePropertyFromSortingCenter(sortingCenter, longProperty);
    }

    @Test
    void findUserPropertyWithoutTransaction() {
        transactionTemplate.execute((t) -> {
            userPropertyService.addPropertyToUser(user, longProperty, 14L);
            return  null;
        });
        long longValue = userPropertyService.findPropertyForUser(longProperty, user);
        assertThat(longValue).isEqualTo(14);
    }
}
