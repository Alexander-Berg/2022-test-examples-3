package ru.yandex.market.stat.dicts.loaders;

import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import static org.hamcrest.MatcherAssert.assertThat;

@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@Slf4j
public class LoadDictionaryITest extends BaseLoadTest {

    private static String DICT_FOR_TEST = "";

    @Test
    public void testLoad() throws Exception {
        Assume.assumeFalse("No dictionary to check, this is ok for automatic run", getDictionary().isEmpty());
        loadDictionary();
    }

    @Test
    public void testNoLoadsForCI() {
        assertThat("Should be no dictionaries to test in master!!", getDictionary(),
                Matchers.is(Matchers.isEmptyOrNullString()));
    }

    @Override
    public String getDictionary() {
        String dict = DICT_FOR_TEST.isEmpty() ? dictionary : DICT_FOR_TEST;
        log.info("======== Running test for {} =====", dict);
        return dict;
    }

}

