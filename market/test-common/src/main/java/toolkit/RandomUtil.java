package toolkit;

import java.util.Base64;
import java.util.Random;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class RandomUtil {

    private static final Random RANDOM = new Random();
    private static final String[] IMEI_REPORTING_BODY_IDS = {"01", "10", "30", "33", "35", "44",
            "45", "49", "50", "51", "52", "53", "54", "86", "91", "98", "99"};
    private static final String WAREHOUSE_PREFIX = "99";

    // формат киза https://wiki.yandex-team.ru/delivery/fulfilment/control-identification-mark/#markencoding
    public static final int CIS_GTIN_LENGTH = 14;
    private static final String CIS_TEMPLATE = "01%s21%s";
    private static final int CIS_SERIES_LENGTH = 20;
    private static final String GS = "#GS#";
    private static final int ES3_CRYPTO_PART_LENGTH = 8;
    private static final int ES4_CRYPTO_PART_LENGTH = 65;
    private static final String CRYPTO_TAIL_TEMPLATE = GS + "91%s" + GS + "92%s";


    public static String randomStringNumbersOnly(int length) {
        StringBuilder result = new StringBuilder();
        while (result.length() < length) {
            result.append(RANDOM.nextInt(1_000_000_000));
        }
        return result.substring(0, length);
    }

    public static int randomInt(int digitsCount) {
        int min = (int) Math.pow(10, digitsCount - 1);
        int max = (int) Math.pow(10, digitsCount) - 1;
        return min + RANDOM.nextInt(max - min + 1);
    }

    private static int sumDigits(int number) {
        int a = 0;
        while (number > 0) {
            a = a + number % 10;
            number = number / 10;
        }
        return a;
    }

    public static String generateImei() {
        String first14 = format("%s%.12s",
                IMEI_REPORTING_BODY_IDS[RANDOM.nextInt(IMEI_REPORTING_BODY_IDS.length)],
                format(ENGLISH, "%012d", abs(RANDOM.nextLong()))
        );

        int sum = 0;

        for (int i = 0; i < first14.length(); i++) {
            int c = Character.digit(first14.charAt(i), 10);
            sum += (i % 2 == 0 ? c : sumDigits(c * 2));
        }

        int finalDigit = (10 - (sum % 10)) % 10;

        return first14 + finalDigit;
    }

    public static Pair<String, String> generateCis(String gtin) {
        final String cisWithoutCryptoTail =
                String.format(CIS_TEMPLATE,
                        StringUtils.isEmpty(gtin) ? RandomStringUtils.randomNumeric(CIS_GTIN_LENGTH) : gtin,
                        RandomStringUtils.randomAlphanumeric(CIS_SERIES_LENGTH));
        final String es3CryptoPart = RandomStringUtils.randomAlphanumeric(ES3_CRYPTO_PART_LENGTH);
        final String es4CryptoPart = Base64
                        .getEncoder()
                        .encodeToString(RandomStringUtils.randomAlphanumeric(ES4_CRYPTO_PART_LENGTH).getBytes());
        final String cisCryptoTail = String.format(CRYPTO_TAIL_TEMPLATE, es3CryptoPart, es4CryptoPart);
        final String cisWithCryptoTail = cisWithoutCryptoTail + cisCryptoTail;
        return Pair.of(cisWithoutCryptoTail, cisWithCryptoTail);
    }

    public static String generateUit() {
        return WAREHOUSE_PREFIX + randomStringNumbersOnly(10);
    }
}
