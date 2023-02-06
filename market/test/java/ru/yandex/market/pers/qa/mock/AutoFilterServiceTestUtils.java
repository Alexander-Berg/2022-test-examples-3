package ru.yandex.market.pers.qa.mock;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.cleanweb.CleanWebContent;
import ru.yandex.market.cleanweb.dto.CleanWebResponseDto;
import ru.yandex.market.cleanweb.dto.VerdictDto;
import ru.yandex.market.pers.qa.model.ModState;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

/**
 * @author korolyov
 * 21.06.18
 */
public class AutoFilterServiceTestUtils {

    public static final String ILLEGAL_WORD = "fuck";
    public static final String BROKE_WORD = "broke";

    public static ModState modStateByUserIdMod3(EntityForFilter entityForFilter) {
        switch ((int) (entityForFilter.userId % 3)) {
            case 0:
                return ModState.AUTO_FILTER_PASSED;
            case 1:
                return ModState.AUTO_FILTER_REJECTED;
            case 2:
                return ModState.AUTO_FILTER_UNKNOWN;
            default:
                throw new IllegalStateException("Never happened");
        }
    }

    public static List<String> generateTextsForFilteringMod3(int count) {
        List<String> strings = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            switch (i % 3) {
                case 0:
                    strings.add("text");
                    break;
                case 1:
                    strings.add("fuck");
                    break;
                case 2:
                    strings.add("broke");
                    break;
            }
        }
        return strings;
    }

    public static List<String> generateTextsForCheckAutoFilterRegexp() {
        return List.of("Телефончик огонь! Сейчас его по акции существенно дешевле продается:\n" +
                "https://swyaznoy.ru-service-discount.ru/catalog/phone/224/6168859",
            "https://svаznоi.ru-nextprices.xyz/catalog/phone/224/6168859",
            "\uD83E\uDD1C\uD83C\uDFFC https://svаznоi.ru-goodprices.xyz/catalog/phone/224/6168859",
            "Телефончик просто супер! Сейчас его по акции значительно дешевле можно купить:\n" +
                "https://sуаznoy.ru-service-discount.ru/catalog/phone/224/6168859", // здесь русская буква у
            "https://svаsnоi.ru-schemenine.ru/catalog/phone/224/6168859",
            "Телефончик просто супер! Сейчас его по акции значительно дешевле можно купить:\n" +
                "https://syaznoy.ru-service-discount.ru/catalog/phone/224/6168859",
            "Телефончик супер! Сейчас на такой очень хорошая акция есть можно по старой цене купить:\n" +
                "https://swiasnoi.ru-actionsjul.top/list/phone/224/6448143");
    }

    public static void mockCleanWebClient(CleanWebClient cleanWebClient) {
        when(cleanWebClient.sendContent(any(CleanWebContent.class), anyBoolean())).then(invocation -> {
            CleanWebContent content = invocation.getArgument(0);
            String text = content.getText();
            String id = content.getId();
            String key = content.getKey();

            if (text.contains(ILLEGAL_WORD)) {
                return getResponse(id, key, true);
            } else if (text.contains(BROKE_WORD)) {
                return getResponse(id, key, null);
            } else {
                return getResponse(id, key, false);
            }
        });
    }

    public static void mockCleanWebClient(CleanWebClient cleanWebClient, ModState modState) {
        when(cleanWebClient.sendContent(any(CleanWebContent.class), anyBoolean())).then(invocation -> {
            CleanWebContent content = invocation.getArgument(0);
            String text = content.getText();
            String id = content.getId();
            String key = content.getKey();

            if (modState == ModState.AUTO_FILTER_REJECTED) {
                return getResponse(id, key, true);
            } else if (modState == ModState.AUTO_FILTER_UNKNOWN) {
                return getResponse(id, key, null);
            } else {
                return getResponse(id, key, false);
            }
        });
    }

    public static CleanWebResponseDto getResponse(String id, String key, Boolean value) {
        VerdictDto[] verdictDtos = null;
        if (value != null) {
            verdictDtos = new VerdictDto[]{new VerdictDto("text_auto_obscene", "clean-web",
                String.valueOf(value), "text", "tmu", key)
            };
        }
        return new CleanWebResponseDto(id, verdictDtos);
    }
}
