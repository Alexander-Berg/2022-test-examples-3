package ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.gapi;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import ru.yandex.personal.mail.search.metrics.scraper.services.scraping.MailSearchSystemLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GApiSearchSystemLoaderTest {

    @Test
    void load() {
        GApiSearchSystemFactory factory = mock(GApiSearchSystemFactory.class);

        GApiSearchSystem service = new GApiSearchSystem(null, null, null, null);
        when(factory.createServiceFromFiles(any(), any())).thenReturn(service);

        MailSearchSystemLoader loader = new GApiSearchSystemLoader(factory);
        assertEquals(service, loader.load(Paths.get("")));
    }
}
