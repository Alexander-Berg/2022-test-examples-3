package ru.yandex.direct.core.entity.keyword.service;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.MODERATE_EVERY_KEYWORD_CHANGE;

@CoreTest
@RunWith(Parameterized.class)
public class KeywordModerationServiceTest {

    private KeywordModerationService keywordModerationService;

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Parameterized.Parameter(0)
    public String oldPhrase;

    @Parameterized.Parameter(1)
    public String newPhrase;

    @Parameterized.Parameter(2)
    public Boolean expectedNeedModerate;

    @Parameterized.Parameter(3)
    public Boolean property;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        keywordModerationService = new KeywordModerationService(ppcPropertiesSupport);
    }

    @Parameterized.Parameters(name = "старая фраза: {0}, новая фраза: {1}, нужна ли перемодерация {2}, " +
            "включена ли переотавка любого изменения фразы {3}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                //Для выключенной проперти MODERATE_EVERY_KEYWORD_CHANGE
                //нормальная форма не поменялась
                {"вакуумный коню", "вакуумному конь", false, false},
                {"[вакуумный коню]", "[вакуумному конь]", false, false},
                {"\"вакуумный коню\"", "\"вакуумному конь\"", false, false},
                {"такой-1xbet-о", "1xbet", false, false},
                {"такой-казино-о на-вулкани-и", "казино вулкан", false, false},

                //охват изменился
                {"вакуумный конь", "вакуумному слону сердитому", true, false},
                {"\"вакуумный конь\"", "\"вакуумному слону сердитому\"", true, false},
                {"слоны [умеют летать] но не очень", "слоны [умеют летать]", true, false},

                //сузился охват
                {"вакуумный конь", "вакуумному коню сердитому", true, false},
                {"\"вакуумный конь\"", "\"вакуумному коню сердитому\"", true, false},
                {"слоны [умеют летать]", "слоны [умеют летать] но не очень", true, false},

                //квадратные скобки считаем как одно слово
                {"[вакуумный конь]", "[вакуумному коню сердитому]", true, false},
                {"вакуумный конь", "вакуумному сердитому [коню]", true, false},

                //сузился охват, но есть горячая фраза
                {"продать слона", "продать слона большого", true, false},
                {"\"продать слона\"", "\"продать слона большого\"", true, false},
                {"продать слона", "\"продать слона\"", true, false},
                {"[продать слона]", "[продать слона большого]", true, false},
                {"продать слона", "продать большого [слона]", true, false},
                {"из-за", "автоматы из-за", true, false}, //исходная фраза не равна нормальной форме
                {"из-за", "!автоматы из-за", true, false}, // с фиксацией

                //расширился охват
                {"вакуумному коню сердитому", "вакуумный конь", true, false},
                {"\"вакуумному коню сердитому\"", "\"вакуумный конь\"", true, false},

                //фраза полностью изменилась
                {"подарки коню", "казино вулкан", true, false},
                {"пластиковые слоны", "aзино777 большие выигрыши", true, false},
                {"вулkановi саsiпо", "вулкан саsinо", true, false},
                {"как на-azino777-и таким что-бы", "казино aзино", true, false},

                //добавление/удаление скобок
                {"\"продать слона\"", "вакуумному конь", true, false},
                {"вакуумный конь", "\"вакуумному конь\"", true, false},
                {"[вакуумный конь]", "вакуумному конь", true, false},
                {"вакуумный конь", "[вакуумному конь]", true, false},
                {"\"вакуумный конь\"", "[вакуумному конь]", true, false},

                //Для включенной проперти MODERATE_EVERY_KEYWORD_CHANGE
                //нормальная форма не поменялась
                {"вакуумный коню", "вакуумному конь", true, true},
                {"[вакуумный коню]", "[вакуумному конь]", true, true},
                {"\"вакуумный коню\"", "\"вакуумному конь\"", true, true},
                {"такой-1xbet-о", "1xbet", true, true},
                {"такой-казино-о на-вулкани-и", "казино вулкан", true, true},

                //охват изменился
                {"вакуумный конь", "вакуумному слону сердитому", true, true},
                {"\"вакуумный конь\"", "\"вакуумному слону сердитому\"", true, true},
                {"слоны [умеют летать] но не очень", "слоны [умеют летать]", true, true},

                //сузился охват
                {"вакуумный конь", "вакуумному коню сердитому", true, true},
                {"\"вакуумный конь\"", "\"вакуумному коню сердитому\"", true, true},
                {"слоны [умеют летать]", "слоны [умеют летать] но не очень", true, true},

                //квадратные скобки считаем как одно слово
                {"[вакуумный конь]", "[вакуумному коню сердитому]", true, true},
                {"вакуумный конь", "вакуумному сердитому [коню]", true, true},

                //сузился охват, но есть горячая фраза
                {"продать слона", "продать слона большого", true, true},
                {"\"продать слона\"", "\"продать слона большого\"", true, true},
                {"продать слона", "\"продать слона\"", true, true},
                {"[продать слона]", "[продать слона большого]", true, true},
                {"продать слона", "продать большого [слона]", true, true},
                {"из-за", "автоматы из-за", true, true}, //исходная фраза не равна нормальной форме
                {"из-за", "!автоматы из-за", true, true}, // с фиксацией

                //расширился охват
                {"вакуумному коню сердитому", "вакуумный конь", true, true},
                {"\"вакуумному коню сердитому\"", "\"вакуумный конь\"", true, true},

                //добавление/удаление скобок
                {"\"продать слона\"", "вакуумному конь", true, true},
                {"вакуумный конь", "\"вакуумному конь\"", true, true},
                {"[вакуумный конь]", "вакуумному конь", true, true},
                {"вакуумный конь", "[вакуумному конь]", true, true},
                {"\"вакуумный конь\"", "[вакуумному конь]", true, true},

                //фраза полностью изменилась
                {"подарки коню", "казино вулкан", true, true},
                {"пластиковые слоны", "aзино777 большие выигрыши", true, true},
                {"вулkановi саsiпо", "вулкан саsinо", true, true},
                {"как на-azino777-и таким что-бы", "казино aзино", true, true},
        });
    }


    @Test
    public void checkPhraseModerate() {
        ppcPropertiesSupport.set(MODERATE_EVERY_KEYWORD_CHANGE.getName(), property.toString());

        KeywordWithMinuses newNormKeywordWithMinuses =
                keywordNormalizer.normalizeKeywordWithMinuses(KeywordParser.parseWithMinuses(newPhrase));
        KeywordWithMinuses oldNormKeywordWithMinuses =
                keywordNormalizer.normalizeKeywordWithMinuses(KeywordParser.parseWithMinuses(oldPhrase));
        KeywordWithMinuses newKeywordWithMinuses = KeywordParser.parseWithMinuses(newPhrase);
        KeywordWithMinuses oldKeywordWithMinuses = KeywordParser.parseWithMinuses(oldPhrase);
        boolean isNeedModerate =
                keywordModerationService.checkModerateKeyword(oldNormKeywordWithMinuses, newNormKeywordWithMinuses,
                        oldKeywordWithMinuses, newKeywordWithMinuses);
        assertThat("результат сравнения соответствует ожиданию",
                isNeedModerate, equalTo(expectedNeedModerate));
    }
}
