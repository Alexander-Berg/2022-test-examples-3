package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.utils;

import com.yandex.xplat.testopithecus.Message;
import com.yandex.xplat.testopithecus.MessageView;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Utils {
    public static List<MessageView> convertHtmlMessagesToModel(ElementsCollection<MessageBlock> htmlMessages) {
        List<MessageView> messages = new LinkedList<>();
        htmlMessages.forEach(messageBlock -> messages.add(
            new Message(
                messageBlock.sender().getText(),
                messageBlock.subject().getText(),
                0,
                "", //TODO: change it
                isMessageThread(messageBlock) ? Integer.parseInt(messageBlock.threadCounter().getText()) : null,
                ru.yandex.autotests.innerpochta.util.Utils.isSubElementAvailable(messageBlock, By.cssSelector(".mail-Icon-Read:not(.is-active)")),
                ru.yandex.autotests.innerpochta.util.Utils.isSubElementAvailable(messageBlock, By.cssSelector("mail" +
                    "-Icon-Importance.is-active")),
                new ArrayList<>()
            )
        ));
        return messages;
    }

    public static boolean isMessageThread(WebElement element) {
        return ru.yandex.autotests.innerpochta.util.Utils.isSubElementAvailable(element, By.cssSelector(".js-thread-toggle"));
    }
}
