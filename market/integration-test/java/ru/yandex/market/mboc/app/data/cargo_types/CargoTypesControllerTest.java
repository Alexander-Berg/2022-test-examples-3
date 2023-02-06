package ru.yandex.market.mboc.app.data.cargo_types;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.mboc.app.BaseWebIntegrationTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.mboc.common.offers.repository.CargoTypeSnapshotRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class CargoTypesControllerTest extends BaseWebIntegrationTestClass {

    @Autowired
    private CargoTypeSnapshotRepository repository;
    private CargoTypeSnapshot cargo1;
    private CargoTypeSnapshot cargo2;
    private CargoTypeSnapshot cargo3;

    @Before
    public void setUp() throws Exception {
        cargo1 = new CargoTypeSnapshot(1L, "1 uno", 10L);
        cargo2 = new CargoTypeSnapshot(2L, "2 dos", 20L);
        cargo3 = new CargoTypeSnapshot(3L, "3 tres", 30L);
        repository.save(Arrays.asList(cargo1, cargo2, cargo3));
    }

    @Test
    @Ignore("MBO-24064")
    public void shouldReturnAll() throws Exception {
        MvcResult listResult = getJson("/api/data/cargo_types");
        List<DisplayCargoType> page = readJson(listResult, new TypeReference<List<DisplayCargoType>>() {
        });
        assertThat(page)
            .containsExactly(new DisplayCargoType(cargo1),
                new DisplayCargoType(cargo2),
                new DisplayCargoType(cargo3));
    }
}
