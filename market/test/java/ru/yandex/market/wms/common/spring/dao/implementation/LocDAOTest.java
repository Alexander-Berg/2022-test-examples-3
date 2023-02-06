package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.LocationType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Loc;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class LocDAOTest extends IntegrationTest {

    private static final String EXISTING_LOC = "STAGE";
    private static final String NON_EXISTING_LOC = "NONSTAGE";

    @Autowired
    private LocDAO locDAO;

    @Test
    @DatabaseSetup("/db/dao/loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/loc/before.xml", assertionMode = NON_STRICT)
    public void findExistingLoc() {
        Optional<Loc> loc = locDAO.findById(EXISTING_LOC);
        assertions.assertThat(loc).isPresent();
        assertions.assertThat(loc.get().getLoc()).isEqualTo(EXISTING_LOC);
        assertions.assertThat(loc.get().getLocationType()).isEqualByComparingTo(LocationType.RECEIPT_TABLE);
    }

    @Test
    @DatabaseSetup("/db/dao/loc/before.xml")
    @ExpectedDatabase(value = "/db/dao/loc/before.xml", assertionMode = NON_STRICT)
    public void findNotExistingLoc() {
        Optional<Loc> loc = locDAO.findById(NON_EXISTING_LOC);
        assertions.assertThat(loc).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/loc/before-buff.xml")
    public void findInputBuffLocsTest() {
        assertions.assertThat(
                locDAO.findInputBuffLocs(LocationType.ST_IN_BUF, List.of("1", "2")).stream()
                        .map(Loc::getLoc)
                        .collect(Collectors.toList())
        ).contains("INPLB11", "INPLB21");
    }
}
