package ru.yandex.direct.grid.core.entity.touchsocdem.converter;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdem;
import ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._0_17;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._18_24;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._25_34;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._35_44;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._45_54;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._55_;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.FEMALE;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.MALE;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_0;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_18;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_25;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_35;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_45;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_INF;

@ParametersAreNonnullByDefault
public class TouchSocdemTestCases {
    private TouchSocdemTestCases() {
    }

    public static List<Case> getCases() {
        return List.of(
                new Case(
                        "[], 0-inf",
                        makeSocdem(emptyList(), AGE_0, AGE_INF),
                        null
                ),
                new Case(
                        "[], 0-25",
                        makeSocdem(emptyList(), AGE_0, AGE_25),
                        makeBidModifier(
                                zeroMultiplier(null, _25_34),
                                zeroMultiplier(null, _35_44),
                                zeroMultiplier(null, _45_54),
                                zeroMultiplier(null, _55_)
                        )
                ),
                new Case(
                        "[], 35-inf",
                        makeSocdem(emptyList(), AGE_35, AGE_INF),
                        makeBidModifier(
                                zeroMultiplier(null, _0_17),
                                zeroMultiplier(null, _18_24),
                                zeroMultiplier(null, _25_34)
                        )
                ),
                new Case(
                        "[], 18-45",
                        makeSocdem(emptyList(), AGE_18, AGE_45),
                        makeBidModifier(
                                zeroMultiplier(null, _0_17),
                                zeroMultiplier(null, _45_54),
                                zeroMultiplier(null, _55_)
                        )
                ),
                new Case(
                        "[FEMALE], 0-inf",
                        makeSocdem(List.of(FEMALE), AGE_0, AGE_INF),
                        makeBidModifier(
                                zeroMultiplier(MALE, null)
                        )
                ),
                new Case(
                        "[FEMALE], 0-25",
                        makeSocdem(List.of(FEMALE), AGE_0, AGE_25),
                        makeBidModifier(
                                zeroMultiplier(FEMALE, _25_34),
                                zeroMultiplier(FEMALE, _35_44),
                                zeroMultiplier(FEMALE, _55_),
                                zeroMultiplier(FEMALE, _45_54),
                                zeroMultiplier(MALE, null)
                        )
                ),
                new Case(
                        "[FEMALE], 45-inf",
                        makeSocdem(List.of(FEMALE), AGE_45, AGE_INF),
                        makeBidModifier(
                                zeroMultiplier(FEMALE, _0_17),
                                zeroMultiplier(FEMALE, _18_24),
                                zeroMultiplier(FEMALE, _25_34),
                                zeroMultiplier(FEMALE, _35_44),
                                zeroMultiplier(MALE, null)
                        )
                ),
                new Case(
                        "[FEMALE], 35_45",
                        makeSocdem(List.of(FEMALE), AGE_35, AGE_45),
                        makeBidModifier(
                                zeroMultiplier(FEMALE, _0_17),
                                zeroMultiplier(FEMALE, _18_24),
                                zeroMultiplier(FEMALE, _25_34),
                                zeroMultiplier(FEMALE, _45_54),
                                zeroMultiplier(FEMALE, _55_),
                                zeroMultiplier(MALE, null)
                        )
                )
        );
    }

    public static class Case {
        private final String name;
        private final GdiTouchSocdem touchSocdem;
        private final BidModifierDemographics bidModifier;

        public Case(String name, GdiTouchSocdem touchSocdem, @Nullable BidModifierDemographics bidModifier) {
            this.name = name;
            this.touchSocdem = touchSocdem;
            this.bidModifier = bidModifier;
        }

        public String getName() {
            return name;
        }

        public GdiTouchSocdem getTouchSocdem() {
            return touchSocdem;
        }

        public BidModifierDemographics getBidModifier() {
            return bidModifier;
        }
    }

    public static GdiTouchSocdem makeSocdem(
            List<GenderType> genders,
            GdiTouchSocdemAgePoint ageLower,
            GdiTouchSocdemAgePoint ageUpper
    ) {
        return makeSocdem(genders, ageLower, ageUpper, false);
    }

    public static GdiTouchSocdem makeSocdem(
            List<GenderType> genders,
            GdiTouchSocdemAgePoint ageLower,
            GdiTouchSocdemAgePoint ageUpper,
            boolean incompleteBidModifier
    ) {
        return new GdiTouchSocdem()
                .withGenders(genders)
                .withAgeLower(ageLower)
                .withAgeUpper(ageUpper)
                .withIncompleteBidModifier(incompleteBidModifier);
    }

    public static BidModifierDemographics makeBidModifier(BidModifierDemographicsAdjustment... adj) {
        return new BidModifierDemographics()
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withEnabled(true)
                .withDemographicsAdjustments(List.of(adj));
    }

    public static BidModifierDemographics makeDisabledBidModifier() {
        return new BidModifierDemographics()
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withEnabled(true)
                .withDemographicsAdjustments(List.of(zeroMultiplier(MALE, _18_24)));
    }

    public static BidModifierDemographicsAdjustment zeroMultiplier(
            @Nullable GenderType gender,
            @Nullable AgeType age
    ) {
        return makeMultiplier(gender, age, 0);
    }

    public static BidModifierDemographicsAdjustment makeMultiplier(
            @Nullable GenderType gender,
            @Nullable AgeType age,
            int percent
    ) {
        return new BidModifierDemographicsAdjustment()
                .withGender(gender)
                .withAge(age)
                .withPercent(percent);
    }
}
