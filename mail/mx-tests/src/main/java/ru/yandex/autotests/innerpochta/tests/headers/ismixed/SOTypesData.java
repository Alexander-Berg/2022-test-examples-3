package ru.yandex.autotests.innerpochta.tests.headers.ismixed;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.addAll;
import static java.util.Collections.shuffle;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.tests.headers.ismixed.SOTypesData.SoTypes.*;

/**
 * User: alex89
 * Date: 01.08.13
 * http://wiki.yandex-team.ru/bitovyeflagiismixed
 * http://wiki.yandex-team.ru/Pochta/Types
 */
public class SOTypesData {
    public static final String SO_TYPE_PREFIX = "SystMetkaSO:";

    public enum SoTypes {
        REGISTRATION("registration", 2),
        PEOPLE("people", 4),
        ETICKET("eticket", 5),
        ESHOP("eshop", 6),
        NOTIFICATION("notification", 7),
        BOUNCE("bounce", 8),
        GREETING("greeting", 12),
        NEWS("news", 13),
        S_GROUPONSITE("s_grouponsite", 14),
        S_DATINGSITE("s_datingsite", 15),
        S_AVIAETICKET("s_aviaeticket", 16),
        S_BANK("s_bank", 17),
        S_SOCIAL("s_social", 18),
        S_TRAVEL("s_travel", 19),
        S_ZDTICKET("s_zdticket", 20),
        S_REALTY("s_realty", 21),
        PERSONALNEWS("personalnews", 22),
        S_ESHOP("s_eshop", 23),
        S_COMPANY("s_company", 24),
        S_JOB("s_job", 25),
        S_GAME("s_game", 26),
        SCHEMA("schema", 27),
        CANCEL("cancel", 28),
        S_TECH("s_tech", 29),
        S_MEDIA("s_media", 30),
        S_ADVERT("s_advert", 31),
        S_PROVIDER("s_provider", 32),
        S_FORUM("s_forum", 33),
        S_MOBILE("s_mobile", 34),
        S_HOTEL("hotel", 35),
        YAMONEY("yamoney", 36),
        S_TRAINING("s_training", 37),
        LIVEMAIL("livemail", 38),
        S_SENDER("s_sender", 39),
        PHISHING("phishing", 40),
        S_TRACKER("s_tracker", 41),
        INVITE("invite", 42),
        S_TAXI("s_taxi", 43),
        S_DELIVERY("s_delivery", 44),
        S_STATE("s_state", 45),
        FIRSTMAIL("firstmail", 46),
        S_NEWS("s_news", 47),
        S_EVENT("s_event", 48),
        EDOC("edoc", 49),
        TRUST_1("trust_1", 51),
        TRUST_2("trust_2", 52),
        TRUST_3("trust_3", 53),
        TRUST_4("trust_4", 54),
        TRUST_5("trust_5", 55),
        TRUST_6("trust_6", 56),
        TRACKERTASK("trackertask", 61),
        DISCOUNT("discount", 62),
        MESSAGE("message", 69),
        PERSONAL("personal", 65),
        BILL("bill", 70),
        REMIND_TIC("remind_tic", 63),
        LOSTCART("lostcart", 71),
        T_NEWS("t_news", 100),
        T_SOCIAL("t_social", 101),
        T_NOTIFICATION("t_notification", 102),
        T_PEOPLE("t_people", 103);

        private String name;
        private int code;

        private SoTypes(String name, int code) {
            this.name = name;
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public static List<SoTypes> getShuffledTypesList() {
            List<SoTypes> randomSetOfTypes = new ArrayList<SoTypes>();
            addAll(randomSetOfTypes, SoTypes.values());
            shuffle(randomSetOfTypes);
            return randomSetOfTypes;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    //https://st.yandex-team.ru/MPROTO-68
    public final static SoTypes[] NO_CHECK_USER_ACTIVITY_SO_TYPES =
            new SoTypes[]{PEOPLE, REGISTRATION, GREETING, ETICKET, ESHOP, S_HOTEL, LIVEMAIL, YAMONEY, CANCEL};

    public static class IsMixedInfo {
        private static final Pattern IS_MIXED_IN_LOG_PATTERN =
                Pattern.compile("is_mixed=(-?\\d+);?,?");
        private static final int MIXED_MAX_BITS_NUMBER = 47;
        public static final long IS_MIXED_MAX = (long) Math.pow((double) 2, (double) MIXED_MAX_BITS_NUMBER) - 1;
        private static final int LABELS_MAX_BITS_NUMBER = 30;
        private static final int LABEL_BITS_SIZE = 6;
        private static final int MAX_NUMBER_OF_LABELS = LABELS_MAX_BITS_NUMBER / LABEL_BITS_SIZE;

        private String bitsFlagIsMixed;
        private List<Integer> labels = new ArrayList<Integer>();

        private IsMixedInfo(long isMixed) {
            this.bitsFlagIsMixed = addZeros(Long.toBinaryString(isMixed));
            this.labels = formSoLabelsInformation(bitsFlagIsMixed);
        }

        public static IsMixedInfo parseIsMixed(long isMixed) {
            assertThat("Слишком большое значение is_mixed!", isMixed, lessThanOrEqualTo(IS_MIXED_MAX));
            return new IsMixedInfo(isMixed);
        }

        public List<Integer> getSoLabels() {
            return labels;
        }

        public String getBitsFlagIsMixed() {
            return bitsFlagIsMixed;
        }

        private static List<Integer> formSoLabelsInformation(String bitsFlagIsMixed) {
            List<Integer> labels = new ArrayList<Integer>();
            for (int i = 0; i < MAX_NUMBER_OF_LABELS; i++) {
                String labelBits = bitsFlagIsMixed.substring(i * LABEL_BITS_SIZE, (i + 1) * LABEL_BITS_SIZE);
                int labelCode = Integer.parseInt(labelBits, 2);
                if (labelCode != 0) {
                    labels.add(Integer.parseInt(labelBits, 2));
                }
            }
            return labels;
        }

        private static String addZeros(String bitsString) {
            return StringUtils.repeat("0", (MIXED_MAX_BITS_NUMBER - bitsString.length())) + bitsString;
        }

        public static String findIsMixedFlagInLog(String log) {
            Matcher serverFormat = IS_MIXED_IN_LOG_PATTERN.matcher(log);
            assertTrue("Не нашли запись о флаге is_mixed в логе!", serverFormat.find());
            return serverFormat.group(1);
        }
    }
}
