package ru.yandex.calendar.logic.beans;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.beans.generated.LayerHelper;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.layer.LayerType;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

public class GenericBeanDaoTest extends AbstractConfTest {

    @Autowired
    private GenericBeanDao genericBeanDao;
    @Autowired
    private SettingsRoutines settingsRoutines;

    private JdbcTemplate3 jdbcTemplate3;

    @Before
    public void setup() {
        jdbcTemplate3 = new JdbcTemplate3(dataSource);
    }

    @Test
    public void update() {
        Layer layer = new Layer();
        layer.setCreatorUid(new PassportUid(123456));
        layer.setType(LayerType.USER);
        layer.setId(genericBeanDao.insertBeanGetGeneratedKey(layer));

        layer.setName("my layer");
        genericBeanDao.updateBean(layer);

        Layer gotLayer = genericBeanDao.loadBeanById(LayerHelper.INSTANCE, layer.getId());
        Assert.A.equals("my layer", gotLayer.getName().getOrNull());
    }

    @Test
    public void insertIgnoreDuplicates() {
        genericBeanDao.insertBeansIgnoreDuplicates(Cf.list(333L, 555L, 333L).map(
                uid -> {
                    Settings s = settingsRoutines.getSettingsByUid(PassportUid.cons(uid)).getCommon().copy();
                    s.setCreationTs(Instant.now());

                    return s;
                }));
    }

    @Test
    public void insertGetGeneratedKey() {
        genericBeanDao.insertBeansBatchGetGeneratedKeys(Cf.list(222L, 444L).map(
                uid -> {
                    Layer layer = new Layer();
                    layer.setCreatorUid(PassportUid.cons(uid));

                    return layer;
                }));
    }
} //~
