package ru.yandex.market.robot.patterns;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 06.07.12
 */
public class UrlPatternGeneratorTest extends Assert {
    @Test
    public void testCreate() throws Exception {
        UrlPatternGenerator generator = new UrlPatternGenerator();

        assertEquals(
            "hello.*", generator.create(
                "hello1", "hello2", "hello3"
            ));
        assertEquals(
            "hello/.*/hi",
            generator.create(
                "hello/555/hi", "hello/55544/hi", "hello/967/hi"
            )
        );
        assertEquals(
            "/articles/Otzivi-vladeltsev/.*",
            generator.create(
                "/articles/Otzivi-vladeltsev/Volkswagen_Touareg_Otzyvy_vladelcev",
                "/articles/Otzivi-vladeltsev/Opel_Astra_Otzyvy_vladelcev",
                "/articles/Otzivi-vladeltsev/Dodge_Nitro_Oksana_Negrii"
            )
        );

        assertEquals(
            "http://www\\.baby\\.ru/company/.*/opinions/.*",
            generator.create(
                "http://www.baby.ru/company/moscow/zhk/45027/opinions/",
                "http://www.baby.ru/company/himki/zhk/45038/opinions/?page=2",
                "http://www.baby.ru/company/moscow/medical_centers/40848/opinions/",
                "http://www.baby.ru/company/moscow/zhk/45027/opinions/?page=1"
            )
        );
    }
}
