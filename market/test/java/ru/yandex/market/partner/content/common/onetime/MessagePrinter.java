package ru.yandex.market.partner.content.common.onetime;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.partner.content.common.message.Messages;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Ignore
public class MessagePrinter {

    @Test
    public void printDcpCodes() {
        getAllMessages().stream()
            .filter(message ->
                message.code().contains(".dcp.")
//                    || message.code().contains(".cw.")
            )
            .map(message -> message.code() + "\t" + message.message())
            .forEach(System.out::println);
    }

    @NotNull
    private List<Messages.MessageTemplate> getAllMessages() {
        List<Messages.MessageTemplate> codes = new ArrayList<>();
        for (Method method : Messages.class.getDeclaredMethods()) {
            if (method.isSynthetic() || Modifier.isStatic(method.getModifiers()) ||
                "recreateMessageInfo".equals(method.getName())) {
                continue;
            }

            Messages.MessageTemplate annotation = method.getAnnotation(Messages.MessageTemplate.class);
            if (annotation == null) {
                System.out.println("Can't find annotation @MessageTemplate on method " + method.getName());
                continue;
            }
            codes.add(annotation);
        }
        return codes;
    }
}
