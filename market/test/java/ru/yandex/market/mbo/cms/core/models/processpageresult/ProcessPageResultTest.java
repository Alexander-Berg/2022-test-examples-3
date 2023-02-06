package ru.yandex.market.mbo.cms.core.models.processpageresult;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.processpageresult.message.GenericMessage;

public class ProcessPageResultTest {

    @Test
    public void getMessageList() {
        ProcessPageResult result = new ProcessPageResult();
        ProcessPageResultMessageInterface w1 = new GenericMessage("Message1", ProcessPageResultMessageLevel.WARNING);
        ProcessPageResultMessageInterface w2 = new GenericMessage("Message2", ProcessPageResultMessageLevel.WARNING);
        ProcessPageResultMessageInterface c1 = new GenericMessage("Message3", ProcessPageResultMessageLevel.CRITICAL);
        ProcessPageResultMessageInterface c2 = new GenericMessage("Message4", ProcessPageResultMessageLevel.CRITICAL);
        ProcessPageResultMessageInterface c3 = new GenericMessage("Message5", ProcessPageResultMessageLevel.CRITICAL);
        result.addMessage(w1);
        result.addMessage(w2);
        result.addMessage(c1);
        result.addMessage(c2);
        result.addMessage(c3);

        Assert.assertTrue(result.getMessageList(ProcessPageResultMessageLevel.WARNING).containsAll(Arrays.asList(w1,
            w2)));
        Assert.assertTrue(result.getMessageList(ProcessPageResultMessageLevel.CRITICAL).containsAll(Arrays.asList(c1,
            c2, c3)));
    }
}
