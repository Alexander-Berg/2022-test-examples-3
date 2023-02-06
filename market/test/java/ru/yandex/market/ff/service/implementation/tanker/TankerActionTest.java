package ru.yandex.market.ff.service.implementation.tanker;

import java.util.Comparator;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.TankerEntry;
import ru.yandex.market.ff.repository.TankerEntryRepository;
import ru.yandex.market.ff.service.TankerEntriesService;

/**
 * Тест для отображения всех загруженных из Танкера ключей (только для локального запуска)
 */

@Disabled
public class TankerActionTest extends IntegrationTest {

    @Autowired
    private TankerEntriesService tankerEntriesService;

    @Autowired
    private TankerEntryRepository tankerEntryRepository;

    @Test
    @DatabaseSetup("classpath:service/tanker/before-loading-keys.xml")
    @Disabled
    public void loadKeys() {
        tankerEntriesService.loadEntries();
        List<TankerEntry> allTankerEntries = tankerEntryRepository.findAll();
        System.out.println("Total keys loaded: " + allTankerEntries.size());
        allTankerEntries.stream()
                .sorted(Comparator.comparing(TankerEntry::getKey))
                .forEach(System.out::println);
    }
}
