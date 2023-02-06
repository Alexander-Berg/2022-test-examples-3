package ru.yandex.market.jmf.logic.def.test;

import java.time.OffsetDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.logic.def.Bo;
import ru.yandex.market.jmf.logic.def.DbVersionService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
abstract class AbstractDbVersionServiceTest {
    protected static final Fqn DUMMY_FQN = Fqn.of("dbVersionServiceDummy");
    protected static final String VERSIONED_STRING_ATTRIBUTE = "versionedString";

    protected static final OffsetDateTime BEFORE_TIME = OffsetDateTime.parse("2020-01-01T00:00:00+00:00");
    protected static final OffsetDateTime CREATE_TIME = OffsetDateTime.parse("2020-01-02T00:00:00+00:00");
    protected static final OffsetDateTime FIRST_EDIT_TIME = OffsetDateTime.parse("2020-01-03T00:00:00+00:00");
    protected static final OffsetDateTime LAST_EDIT_TIME = OffsetDateTime.parse("2020-01-04T00:00:00+00:00");
    protected static final OffsetDateTime AFTER_TIME = OffsetDateTime.parse("2020-01-05T00:00:00+00:00");

    protected static final EntityValues entityValues = new EntityValues();
    protected final EntityValues[] entityValuesArray = new EntityValues[]{
            entityValues,
            new EntityValues(),
            new EntityValues()
    };

    protected String entityGid;
    protected String[] entityGidsArray = new String[3];

    @Inject
    DbVersionService dbVersionService;
    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;

    @BeforeEach
    public void setUp() {
        for (int i = 0; i < entityValuesArray.length; i++) {
            entityGidsArray[i] = createAndEdit(entityValuesArray[i]);
        }
        entityGid = entityGidsArray[0];

        // Сущности, для "шума"
        createAndEdit(new EntityValues());
        createAndEdit(new EntityValues());
    }

    private String createAndEdit(EntityValues values) {
        String gid = create(values.createValue, CREATE_TIME);
        edit(gid, values.firstEditValue, FIRST_EDIT_TIME);
        edit(gid, values.lastEditValue, LAST_EDIT_TIME);
        return gid;
    }

    @SuppressWarnings("SameParameterValue")
    private String create(String value, OffsetDateTime processTime) {
        return bcpService.create(
                DUMMY_FQN,
                Maps.of(
                        Bo.TITLE, "Dummy",
                        VERSIONED_STRING_ATTRIBUTE, value),
                processTime)
                .getGid();
    }

    private void edit(String gid, String value, OffsetDateTime processTime) {
        bcpService.edit(gid, Maps.of(VERSIONED_STRING_ATTRIBUTE, value), processTime);
    }

    public static class EntityValues {
        public final String createValue = Randoms.string();
        public final String firstEditValue = Randoms.string();
        public final String lastEditValue = Randoms.string();
    }

}
