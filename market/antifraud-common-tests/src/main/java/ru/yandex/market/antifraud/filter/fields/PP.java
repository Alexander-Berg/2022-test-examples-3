package ru.yandex.market.antifraud.filter.fields;

import ru.yandex.market.antifraud.filter.RndUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: jkt
 * Date: 05.07.13
 * Time: 16:08
 * wiki: https://wiki.yandex-team.ru/market/development/placement
 */
public class PP {

    private static final int[] MARKET_PP = {
            1, 2, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28,
            33, 34, 35, 37, 38, 39,
            41, 42, 43, 44, 46, 47, 48, 49,
            59,
            61, 62, 63, 64, 65, 68, 69,
            101, 102, 103, 104, 105, 106,
            140, 141, 142, 143, 144, 145, 146, 147,
            155,
            160, 161,
            170, 171,  172,  173,  174,  175,  176,  177,  178, 179,
            180, 184, 188, 189,
            200, 201, 202, 203, 204, 205, 206, 207, 208, 209,
            210, 211, 214, 215, 216, 217, 218,
            238, 239, 240, 242, 245, 250,
            270, 272, 275, 279,
            280, 284, 288, 289, 292, 295,
            404, 405, 406, 407, 410,
            606, 610, 613, 622, 625, 628, 630, 660,
            706, 707, 710, 713, 721, 722, 725, 726, 728, 730, 750, 760,
            806, 807, 810, 813, 821, 822, 825, 826, 828, 830, 850, 860,
            900, 901, 902, 903, 904, 905, 906, 910,
            1000, 1001, 1002
    };
    private static final int[] CPA_PP = {
            6, 7,
            13,
            21, 22, 23, 28,
            46, 47, 48, 49,
            61, 62, 63, 64, 68, 69,
            104, 105,
            144, 145,
            179,
            180, 184, 188, 189,
            200, 201, 203, 204, 205, 206, 207, 208, 209,
            210, 211,
            238, 239,
            279,
            280, 284, 288, 289,
            410,
            606, 610, 613, 622, 625, 628, 630, 660,
            706, 707, 710, 713, 721, 722, 725, 726, 728, 730, 750, 760,
            806, 807, 810, 813, 821, 822, 825, 826, 828, 830, 850, 860,
            900, 901, 902, 903, 904, 905, 906, 910
    };

    private static final int[] PP_OI = {1, 2, 3, 4, 5, 6};

    private static final int[] MARKET_CARD_PP =  {6, 13, 21, 143, 7, 8, 22, 24, 25, 26, 27, 28, 35, 144,
            203, 238, 239, 240, 61, 62, 63, 64};
    private static final int[] MARKET_NO_CPA_PP = {8, 143, 24, 25, 26, 27, 35, 240};
    private static final int[] NO_MARKET_CARD_PP = {200, 201, 204, 205, 206, 207, 208, 220, 221};
    private static final int[] MARKET_NOT_REPORT_PP = {143, 7, 8, 22, 24, 25, 26, 27, 28, 35, 144,
            200, 203, 238, 239, 240, 61, 62, 63, 64};
    private static final int[] VENDOR_CLICKS_PP = {7, 402, 403, 405, 47, 38};
    private static final int[] TYRKEY_PP = {301, 302, 303, 304, 305, 306, 307, 313};
    private static final int[] TYRKEY_GRADE_PP = {308, 309, 310, 311, 312};

    public static int getBooknowPP() {
        return 251;
    }

    public static int getRandomMarketPP() {
        return RndUtil.choice(MARKET_PP);
    }

    public static int getRandomMarketCardPP() {
        return RndUtil.choice(MARKET_CARD_PP);
    }

    public static int getRandomMarketNoCpaPP() {
        return RndUtil.choice(MARKET_NO_CPA_PP);
    }

    public static int getRandomNoMarketCardPP() {
        return RndUtil.choice(NO_MARKET_CARD_PP);
    }

    public static int getRandomMarketNotReportPP() {
        return RndUtil.choice(MARKET_NOT_REPORT_PP);
    }

    public static int getRandomCpaPP() {
        return RndUtil.choice(CPA_PP);
    }

    public static int getRandomTurkeyPP() {
        return RndUtil.choice(TYRKEY_PP);
    }

    public static int getRandomPPOi() {
        return RndUtil.choice(PP_OI);
    }

    public static int getRandomVendorClicks() {
        return RndUtil.choice(VENDOR_CLICKS_PP);
    }

    public static int getMarketNotDirectPP() {
        Set<Integer> pps = new HashSet(Arrays.asList(MARKET_PP));
        Set<Integer> directPPs = new HashSet(Arrays.asList(1, 9, 11, 20, 1000));
        pps.removeAll(directPPs);
        return RndUtil.choice(pps);
    }

    //method for getting different values of array which never fails
    public static int getMarketPPForIndexOrRemainder(int index) {
        if (index < 0) {
            index = -index;
        }
        if (index >= MARKET_PP.length) {
            return MARKET_PP[index % MARKET_PP.length];
        }
        return MARKET_PP[index];
    }
}
