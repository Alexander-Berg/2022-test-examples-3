package ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnly;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTablet;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.container.BidModifierKey;
import ru.yandex.direct.core.entity.bidmodifiers.repository.FakeBidModifierRepository;
import ru.yandex.direct.dbutil.ShardByClient;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.testing.FakeShardByClient;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.DESKTOP_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.DESKTOP_ONLY_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.MOBILE_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.SMARTTV_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.TABLET_MULTIPLIER;

public class DeviceModifiersConflictCheckerTest {

    private ClientId clientId;
    private ShardByClient shardHelper;
    private List<BidModifier> modifiersInOperation;
    private List<BidModifier> modifiersToTest;
    private BidModifier modifierToTest;
    private List<BidModifier> modifiersInDB;

    DeviceModifiersConflictChecker getService(List<BidModifier> modifiers) {
        var repository = new FakeBidModifierRepository(modifiers);
        return new DeviceModifiersConflictChecker(
                shardHelper, repository
        );
    }

    Map<BidModifierKey, BidModifier> listToMap(List<BidModifier> modifiers) {
        return StreamEx.of(modifiers)
                .mapToEntry(BidModifierKey::new, Function.identity())
                .toMap();
    }

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        shardHelper = new FakeShardByClient();
    }

    void setUpMobileMultiplierOperationWithDesktopOnlyAndTablet() {
        modifiersInDB = ImmutableList.of();
        modifierToTest = new BidModifierMobile()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(MOBILE_MULTIPLIER);
        modifiersToTest = ImmutableList.of(modifierToTest);

        modifiersInOperation = new ArrayList<>(modifiersToTest);
        modifiersInOperation.add(new BidModifierDesktopOnly()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(DESKTOP_ONLY_MULTIPLIER));
        modifiersInOperation.add(new BidModifierTablet()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(TABLET_MULTIPLIER));

    }

    @Test
    public void detectsNoConflictingModifiers_whenBaseCheckPasses() {
        setUpMobileMultiplierOperationWithDesktopOnlyAndTablet();

        /*
         * Should provide no conflicts if base type check is false
         */
        Predicate<BidModifier> checkForBaseType = modifier -> false;

        /*
         * These checks would have failed if not for base check
         */
        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                DESKTOP_ONLY_MULTIPLIER, m -> true,
                TABLET_MULTIPLIER, m -> true
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks
        );

        assertThat(result).isEqualTo(emptySet());
    }

    @Test
    public void detectsNoConflictingModifiers_whenAdjacentChecksPass() {
        setUpMobileMultiplierOperationWithDesktopOnlyAndTablet();

        /*
         * If base type check is true - we should depend on adjacent checks
         */
        Predicate<BidModifier> checkForBaseType = modifier -> true;

        /*
         * Since all adjacent checks return false - we should pass
         */
        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                DESKTOP_ONLY_MULTIPLIER, m -> false,
                TABLET_MULTIPLIER, m -> false
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks
        );

        assertThat(result).isEqualTo(emptySet());
    }

    @Test
    public void detectsConflictOnOperationLevel_whenAllAdjacentChecksFail() {
        setUpMobileMultiplierOperationWithDesktopOnlyAndTablet();

        /*
         * If base type check is true - we should depend on adjacent checks
         */
        Predicate<BidModifier> checkForBaseType = modifier -> true;

        /*
         * Looks like situation, when we are trying to set everything to 0
         */
        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                DESKTOP_ONLY_MULTIPLIER, m -> true,
                TABLET_MULTIPLIER, m -> true
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks
        );

        /*
         * Should fail this validation
         */
        assertThat(result).isEqualTo(Set.of(new BidModifierKey(modifierToTest)));
    }

    void setUpMobileMultiplierOperationWithDesktopOnlyAndTabletNotEverythingInOperation() {
        modifiersInDB = new ArrayList<>();
        modifierToTest = new BidModifierMobile()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(MOBILE_MULTIPLIER);
        modifiersToTest = ImmutableList.of(modifierToTest);

        modifiersInOperation = new ArrayList<>(modifiersToTest);
        modifiersInOperation.add(new BidModifierTablet()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(TABLET_MULTIPLIER).withEnabled(false));

    }

    @Test
    public void detectsNoConflictsOnDBLevel_notEveryModifierInOperation() {
        setUpMobileMultiplierOperationWithDesktopOnlyAndTabletNotEverythingInOperation();

        Predicate<BidModifier> checkForBaseType = modifier -> true;

        /*
         * We are checking, that check functions do not get null values
         */
        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                DESKTOP_ONLY_MULTIPLIER, BidModifier::getEnabled,
                TABLET_MULTIPLIER, BidModifier::getEnabled
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks
        );

        /*
         * Should fail this validation
         */
        assertThat(result).isEqualTo(emptySet());
    }

    void setUpDesktopMultiplierOperationWithDesktopOnlyInDb() {
        modifiersInDB = new ArrayList<>();
        modifiersInDB.add(new BidModifierDesktopOnly()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(DESKTOP_ONLY_MULTIPLIER));

        modifierToTest = new BidModifierDesktop()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(DESKTOP_MULTIPLIER);
        modifiersToTest = ImmutableList.of(modifierToTest);

        modifiersInOperation = new ArrayList<>(modifiersToTest);
    }

    @Test
    public void detectsConflictsOnDBLevel_notEveryModifierInOperation() {
        setUpDesktopMultiplierOperationWithDesktopOnlyInDb();

        Predicate<BidModifier> checkForBaseType = modifier -> true;

        /*
         * DESKTOP_ONLY and TABLET always conflicts with DESKTOP
         */
        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                DESKTOP_ONLY_MULTIPLIER, m -> true,
                TABLET_MULTIPLIER, m -> true
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks,
                true
        );

        /*
         * Should fail this validation
         */
        assertThat(result).isEqualTo(Set.of(new BidModifierKey(modifierToTest)));
    }

    void setUpDesktopMultiplierOperationWithMobileInOperationAndSmartTvInDB() {
        modifiersInDB = new ArrayList<>();
        modifiersInDB.add(new BidModifierMobile()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(MOBILE_MULTIPLIER));

        modifierToTest = new BidModifierDesktop()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(DESKTOP_MULTIPLIER);
        modifiersToTest = ImmutableList.of(modifierToTest);

        modifiersInOperation = new ArrayList<>(modifiersToTest);
        modifiersInOperation.add(new BidModifierSmartTV()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(SMARTTV_MULTIPLIER));
    }

    @Test
    public void detectsConflictsOnBothLevels() {
        setUpDesktopMultiplierOperationWithMobileInOperationAndSmartTvInDB();

        Predicate<BidModifier> checkForBaseType = modifier -> true;

        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                MOBILE_MULTIPLIER, m -> true,
                SMARTTV_MULTIPLIER, m -> true
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks
        );

        /*
         * Should fail this validation
         */
        assertThat(result).isEqualTo(Set.of(new BidModifierKey(modifierToTest)));
    }

    void setUpDesktopMultiplierOperationWithMobileAndSmartTvInDB() {
        modifiersInDB = new ArrayList<>();
        modifiersInDB.add(new BidModifierMobile()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(MOBILE_MULTIPLIER));
        modifiersInDB.add(new BidModifierSmartTV()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(SMARTTV_MULTIPLIER));

        modifierToTest = new BidModifierDesktop()
                .withId(1L).withCampaignId(1L).withAdGroupId(1L).withType(DESKTOP_MULTIPLIER);
        modifiersToTest = ImmutableList.of(modifierToTest);

        modifiersInOperation = new ArrayList<>(modifiersToTest);
    }

    @Test
    public void detectsConflictsOnDBLevel() {
        setUpDesktopMultiplierOperationWithMobileAndSmartTvInDB();

        Predicate<BidModifier> checkForBaseType = modifier -> true;

        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                MOBILE_MULTIPLIER, m -> true,
                SMARTTV_MULTIPLIER, m -> true
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks
        );

        /*
         * Should fail this validation
         */
        assertThat(result).isEqualTo(Set.of(new BidModifierKey(modifierToTest)));
    }

    @Test
    public void passesIfAtLeastOneCheckPassesInAllShouldFailMode() {
        setUpDesktopMultiplierOperationWithMobileAndSmartTvInDB();

        Predicate<BidModifier> checkForBaseType = modifier -> true;

        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                MOBILE_MULTIPLIER, m -> true,
                SMARTTV_MULTIPLIER, m -> false
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks
        );

        assertThat(result).isEqualTo(emptySet());
    }


    @Test
    public void failsIfAtLeastOneCheckFailsInAtLeastOneShouldFailMode() {
        setUpDesktopMultiplierOperationWithMobileAndSmartTvInDB();

        Predicate<BidModifier> checkForBaseType = modifier -> true;

        Map<BidModifierType, Predicate<BidModifier>> adjacentTypesChecks = Map.of(
                MOBILE_MULTIPLIER, m -> true,
                SMARTTV_MULTIPLIER, m -> false
        );

        var service = getService(modifiersInDB);
        var result = service.findConflictingModifiers(
                clientId,
                modifiersToTest,
                listToMap(modifiersInOperation),
                checkForBaseType,
                adjacentTypesChecks,
                true
        );

        assertThat(result).isEqualTo(Set.of(new BidModifierKey(modifierToTest)));
    }
}
