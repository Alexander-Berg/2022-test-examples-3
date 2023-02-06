package ru.yandex.market.abo.html;

import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;

import one.util.streamex.StreamEx;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author komarovns
 * @date 15.10.2020
 */
public class CsrfHtmlTest {

    /**
     * В post/put/delete формах должен быть инпут с csrf токеном.
     * если у формы есть атрибут th:action, то спринг сам его вставляет
     */
    @Test
    void formActionTest() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        var resources = resolver.getResources("classpath*:/ru/yandex/market/abo/WEB-INF/templates/**/*.html");
        assertTrue(resources.length > 0);
        for (var htmlResource : resources) {
            assertFalse(
                    hasFormWithAnyMethodAndWithoutActionPrefix(htmlResource),
                    "found form action without th prefix in file " + htmlResource.getURI().getPath()
            );
        }
    }


    /**
     * Проверяем, что нет форм, в которых явно указан method и нет атрибута th:action
     * почему не проверяем какой именно метод? чтобы не париться
     * его можно указать в том числе через th:method => взять в кавычки и делать другие непотребства.
     */
    private boolean hasFormWithAnyMethodAndWithoutActionPrefix(Resource htmlResource) {
        Document html;
        try (var is = htmlResource.getInputStream();
             var bis = new BufferedInputStream(is)) {
            html = Jsoup.parse(bis, StandardCharsets.UTF_8.name(), "localhost");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return StreamEx.of(html.select("form"))
                .anyMatch(form -> (form.hasAttr("method") || form.hasAttr("th:method")) && !form.hasAttr("th:action"));
    }
}
