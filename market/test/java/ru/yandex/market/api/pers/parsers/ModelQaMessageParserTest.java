package ru.yandex.market.api.pers.parsers;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.pers.data.ModelQaMessage;
import ru.yandex.market.api.util.DateUtil;
import ru.yandex.market.common.Parser;
import ru.yandex.market.api.util.ResourceHelpers;

import java.time.LocalDateTime;

public class ModelQaMessageParserTest extends UnitTestBase {

    @Test
    public void getParsed() {
        ModelQaMessage a = parse("pers-qa_answers__answer.json");

        Assert.assertEquals(1415332L, a.getId());
        Assert.assertEquals(540011985L, a.getUserId());
        Assert.assertEquals("За эти деньги лучше нет. Да и если брать дороже модели, то там не для всех будут преимущества, т.к. у того же Redmi Note 5 - экран 2.5D (защитное стекло не наклеишь нормально), нет отдельного слота под MicroSD (а у Redmi S2 он есть), и не всем нужен металлический корпус, т.к. зимы у нас бывают холодные и батарея смартфонов обернутых в металл начинает терять заряд, даже если смартфон выключен.", a.getText());
        Assert.assertEquals(
            LocalDateTime.from(DateUtil.asLocalDateTime(1531867028000L)),
            a.getCreated()
        );
    }

    private ModelQaMessage parse(String filename) {
        Parser<ModelQaMessage> parser = new ModelQaAnswerParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
