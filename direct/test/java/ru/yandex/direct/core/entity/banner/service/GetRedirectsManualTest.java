package ru.yandex.direct.core.entity.banner.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.constraint.StringConstraints.isValidHref;

/**
 * Перед локальным запуском поднять туннель
 * <pre>
 * ssh -L localhost:8166:zora-online.yandex.net:8166 ppcdev1
 * </pre>
 */
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
@ContextConfiguration(classes = CoreConfiguration.class)
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GetRedirectsManualTest {
    @Autowired
    private BannerUrlCheckService bannerUrlCheckService;

    private final File inputFile = new File("get_redirects_input.csv");
    private final File outputFile = new File("get_redirects_output.csv");

    @Test
    public void getRedirects() throws IOException {
        List<String> urls = FileUtils.readLines(inputFile, "utf-8");
        List<String> outputLines = mapList(urls, this::processHref);
        FileUtils.writeLines(outputFile, "utf-8", outputLines);
    }

    private String processHref(String url) {
        if (StringUtils.isBlank(url) || !isValidHref(url)) {
            return String.join(",", url, null, "0");
        }
        RedirectCheckResult result = bannerUrlCheckService.getRedirect(url);

        return String.join(",",
                url,
                result.getRedirectDomain(),
                result.isSuccessful() ? "1" : "0");
    }
}
