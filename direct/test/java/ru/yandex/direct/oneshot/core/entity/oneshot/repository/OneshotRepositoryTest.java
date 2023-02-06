package ru.yandex.direct.oneshot.core.entity.oneshot.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.core.configuration.OneshotCoreTest;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.testing.TestOneshots;
import ru.yandex.direct.oneshot.core.model.Oneshot;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppcdict.tables.Oneshots.ONESHOTS;

@OneshotCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OneshotRepositoryTest {

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private OneshotRepository oneshotRepository;

    @Before
    public void before() {
        dslContextProvider.ppcdict().deleteFrom(ONESHOTS).execute();
    }

    @Test
    public void testGetAll() {
        Oneshot newOneshot = TestOneshots.defaultOneshot();
        oneshotRepository.add(dslContextProvider.ppcdict(), List.of(newOneshot));

        List<Oneshot> oneshotList = oneshotRepository.getAll();
        assertThat(oneshotList.get(0).getClassName(), is(newOneshot.getClassName()));
    }
}
