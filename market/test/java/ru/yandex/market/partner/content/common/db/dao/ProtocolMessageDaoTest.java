package ru.yandex.market.partner.content.common.db.dao;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.ProtocolMessage;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;

import java.util.Arrays;
import java.util.List;

public class ProtocolMessageDaoTest extends BaseDbCommonTest {

    @Autowired
    ProtocolMessageDao protocolMessageDao;

    @Test
    public void insert() {
        final MessageInfo message = Messages.get().excelWrongFileFormat("message");
        final ProtocolMessage protocolMessage = protocolMessageDao.insert(message);
        Assert.assertEquals(message.getCode(), protocolMessage.getCode());
        Assert.assertEquals(message.getParams(), protocolMessage.getParams());
        Assert.assertEquals(message.getLevel(), MessageInfo.Level.ERROR);
    }

    @Test
    public void getMessageInfo() {
        final MessageInfo message = Messages.get().excelWrongFileFormat("message");
        final ProtocolMessage protocolMessage = protocolMessageDao.insert(message);
        final MessageInfo recreatedMessageInfo = protocolMessageDao.getMessageInfo(protocolMessage.getId());
        Assert.assertEquals(message.getCode(), recreatedMessageInfo.getCode());
        Assert.assertEquals(message.getParams(), recreatedMessageInfo.getParams());
        Assert.assertEquals(message.getLevel(), MessageInfo.Level.ERROR);
    }

    @Test
    public void getMessageInfoOtherLevel() {
        final MessageInfo message = Messages.get(MessageInfo.Level.INFO).excelWrongFileFormat("message");
        final ProtocolMessage protocolMessage = protocolMessageDao.insert(message);
        final MessageInfo recreatedMessageInfo = protocolMessageDao.getMessageInfo(protocolMessage.getId());
        Assert.assertEquals(message.getCode(), recreatedMessageInfo.getCode());
        Assert.assertEquals(message.getParams(), recreatedMessageInfo.getParams());
        Assert.assertEquals(message.getLevel(), MessageInfo.Level.INFO);
    }

    @Test
    public void batchInsert() {
        final MessageInfo message1 = Messages.get().skuNotExistSku("notExistSku1");
        final MessageInfo message2 = Messages.get(MessageInfo.Level.WARNING).skuWrongPicture("shopSkuId2", "someUrl");
        List<MessageInfo> messageList = Arrays.asList(message1, message2);
        List<Long> ids = protocolMessageDao.batchInsert(messageList);
        Assert.assertEquals(2, ids.size());
        final MessageInfo recreatedMessageInfo1 = protocolMessageDao.getMessageInfo(ids.get(0));
        Assert.assertEquals(message1.getCode(), recreatedMessageInfo1.getCode());
        Assert.assertEquals(message1.getParams(), recreatedMessageInfo1.getParams());
        Assert.assertEquals(message1.getLevel(), MessageInfo.Level.ERROR);
        final MessageInfo recreatedMessageInfo2 = protocolMessageDao.getMessageInfo(ids.get(1));
        Assert.assertEquals(message2.getCode(), recreatedMessageInfo2.getCode());
        Assert.assertEquals(message2.getParams(), recreatedMessageInfo2.getParams());
        Assert.assertEquals(message2.getLevel(), MessageInfo.Level.WARNING);
    }
}