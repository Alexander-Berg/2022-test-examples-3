package ru.yandex.autotests.direct.web.api.tests.retargeting;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.CryptaGoalsRecord;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.CryptaGoalWeb;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Локализация списка сегментов крипты для ретаргетинга")
@Stories(TestFeatures.Retargeting.GOALS)
@Features(TestFeatures.RETARGETING)
@Tag(TrunkTag.YES)
@Tag(Tags.RETARGETING)
@RunWith(Parameterized.class)
public class CryptaGoalsLocalizationTest {

    @Parameterized.Parameters(name = "язык локализации - {0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"ru"},
                {"en"},
                {"tr"},
        };
        return Arrays.asList(data);
    }

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @Rule
    public DirectRule directRule = DirectRule.defaultRule().as(Logins.DEFAULT_CLIENT);

    @Parameterized.Parameter(0)
    public String lang;

    @Test
    public void allCryptaSegmentsShouldHaveLocalizedNameAndDescription() {
        final List<CryptaGoalsRecord> cryptaGoalsRecords =
                directRule.dbSteps().cryptaGoalsSteps().getCryptaGoalsRecords();
        final Map<Long, String> tankerNameKeyMap = cryptaGoalsRecords.stream()
                .collect(toMap(CryptaGoalsRecord::getGoalId, CryptaGoalsRecord::getTankerNameKey));
        final Map<Long, String> tankerDescKeyMap = cryptaGoalsRecords.stream()
                .collect(toMap(CryptaGoalsRecord::getGoalId, CryptaGoalsRecord::getTankerDescriptionKey));
        final List<CryptaGoalWeb> segments = directRule.webApiSteps().cryptaSteps().getGoals(null, null, lang);
        // фильтруем по id > 0, т.к. ручка /web-api/targets/crypta/segments
        // может возвращать фиктивные сегменты (не хранятся в базе)
        List<CryptaGoalWeb> segmentsWithoutTranslation = segments.stream()
                .filter(s -> s.getId() > 0 && !(s.getName() != null && !tankerNameKeyMap.get(s.getId()).equals(s.getName())
                        && s.getDescription() != null && !tankerDescKeyMap.get(s.getId()).equals(s.getDescription())))
                .collect(Collectors.toList());
        assertThat(
                "Все сегменты крипты локализованы (пока нет локализации на тип audio_genres, поэтому нормально,"
                        + " если тест падает из-за них, пока необходимо проверять вручную, что это не из-за чего-то другого. "
                        + "Переводы для audio_genres будут сделаны в DIRECT-97622)",
                segmentsWithoutTranslation, empty());
    }

}
