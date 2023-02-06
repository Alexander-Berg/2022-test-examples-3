package ru.yandex.market.tsum.registry.v2.dao;

import java.util.List;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.registry.v2.TestInstallationBuilder;
import ru.yandex.market.tsum.registry.v2.dao.model.Installation;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 18/05/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, InstallationsDao.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class InstallationsDaoTest {

    @Autowired
    private InstallationsDao installationsDao;

    @Test
    public void testSaveGetDelete() throws Exception {
        Installation newInstallation = new TestInstallationBuilder()
            .withName("test_installation")
            .withComponentId("componentId")
            .withEnvironment(Environment.TESTING)
            .build();
        installationsDao.save(newInstallation);
        Assert.assertNotNull(newInstallation.getId());

        Installation installationFromMongo = installationsDao.get(newInstallation.getId());
        Assert.assertEquals(newInstallation.getId(), installationFromMongo.getId());

        List<String> installations = installationsDao.getByComponentId("componentId");
        Assert.assertEquals(newInstallation.getId(), installations.get(0));

        installationsDao.remove(newInstallation.getId());
        Assert.assertNull(installationsDao.get(newInstallation.getId()));
    }
}
