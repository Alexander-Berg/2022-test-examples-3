package ru.yandex.market.logistics.iris.repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.entity.Dummy;

public class DummyRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DummyRepository dummyRepository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/dummy/1.xml")
    public void findAllDummies() {
        List<Dummy> all = dummyRepository.findAll();

        Collection<Long> ids = all.stream().map(Dummy::getId).collect(Collectors.toList());

        assertions().assertThat(ids).containsExactlyInAnyOrder(10L, 20L, 30L);
    }
}
