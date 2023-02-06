package ru.yandex.market.mbo.core.title;

import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TitleMakerTemplateUtilTest {

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testOptionIdsParse() {
        String template = "{\"values\":[[(v4940921 == o13475069 ),(\"Смартфон \" )," +
            "(\"Телефон \" )],[(1 ),(v7893318 ),null,(true)],[(v16232166 )," +
            "(\".\" ),(\" \" )],[(1 ),(t0 ),null,(true)],[(v16815063 == \"true\" ),(\" Android One\" )]]}";

        Set<Long> valueIds = TitleMakerTemplateUtil.parseValueIdsFromTemplate(template);

        assertThat(valueIds).containsExactlyInAnyOrder(13475069L);
    }
}
