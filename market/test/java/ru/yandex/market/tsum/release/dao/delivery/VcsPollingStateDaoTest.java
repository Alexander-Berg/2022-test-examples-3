package ru.yandex.market.tsum.release.dao.delivery;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.core.TestMongo;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.01.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {VcsPollingStateDaoTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class VcsPollingStateDaoTest {
    private static final String REPOSITORY_NAME = "market-infra/test";
    private static final String BRANCH = "master";
    @Autowired
    private VcsPollingStateDao sut;

    @Test
    public void nonExistingRevision() {
        Assert.assertFalse(sut.getLastCheckedRevision(REPOSITORY_NAME, BRANCH).isPresent());
    }

    @Test
    public void saveThenGetStableRevision() {
        sut.setLastCheckedRevision(REPOSITORY_NAME, BRANCH, "1");
        Assert.assertEquals("1", sut.getLastCheckedRevision(REPOSITORY_NAME, BRANCH).get());
    }

    @Configuration
    @Import(TestMongo.class)
    public static class Config {
        @Autowired
        private MongoTemplate mongoTemplate;

        @Bean
        public VcsPollingStateDao deliveryMachineStateDao() {
            return new VcsPollingStateDao(mongoTemplate);
        }
    }
}
