package ru.yandex.market.mboc.common.honestmark;

public class HonestMarkTestUtils {

    public static final long HID_1 = 5555L;
    public static final long OTHER_HID = 7777L;
    public static final Long NULL_HID = null;
    public static final String BOOTS_NAME = "boots";
    public static final int BOOTS_ID = 1;

    public static final double DEFAULT_CLASSIFIER =
        OfferCategoryRestrictionCalculator.DEFAULT_CLASSIFIER_TRUST_THRESHOLD;
    public static final double CONFIDENT_CLASSIFIER = DEFAULT_CLASSIFIER + 0.01;
    public static final double NOT_CONFIDENT_CLASSIFIER = DEFAULT_CLASSIFIER - 0.01;

    public static final double ALLOW_GC_THRESHOLD = 0.5;
    public static final double ALLOW_GC_THRESHOLD_ZERO = 0.0;
    public static final double NOT_CONFIDENT_CLASSIFIER_NO_GC = ALLOW_GC_THRESHOLD - 0.1;

    public static final double CONFIDENT_DEP =
        OfferCategoryRestrictionCalculator.HONEST_MARK_DEPARTMENT_TRUST_THRESHOLD + 0.01;
    public static final double NOT_CONFIDENT_DEP =
        OfferCategoryRestrictionCalculator.HONEST_MARK_DEPARTMENT_TRUST_THRESHOLD - 0.01;
}
