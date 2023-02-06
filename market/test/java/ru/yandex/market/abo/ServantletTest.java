package ru.yandex.market.abo;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.framework.core.Servantlet;
import ru.yandex.common.framework.core.validator.AbstractValidatedServantlet;
import ru.yandex.common.framework.http.HttpServRequest;
import ru.yandex.common.framework.http.HttpServResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author frenki
 * created on 09.11.2017.
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles({"functionalTest", "development"})
@ContextConfiguration("classpath:servantlet-test-bean.xml")
public class ServantletTest {
    static {
        System.setProperty("environment", "development");
        System.setProperty("spring.profiles.active", "development");
    }

    @Autowired
    List<AbstractValidatedServantlet> servantletList;

    @Test
    @SuppressWarnings("unchecked")
    public void testServantlets() {
        List<String> errorMessages = new ArrayList<>();
        for (Servantlet servantlet : servantletList) {
            try {
                servantlet.process(new HttpServRequest(0L, "", ""),
                        new HttpServResponse(null, null, null));
            } catch (BadSqlGrammarException ex) {
                errorMessages.add(ex.getMessage());
            } catch (Throwable ex) {
                // Возможно, если передавать какие-то параметры не по умолчанию, всё будет ок и сюда не попадем.
                // Но тест направлен на проверку корректности sql-запросов в сервантлетах, поэтому игнорируем все остальные ошибки.
                System.out.println(ex.getMessage());
            }
        }
        assertTrue(errorMessages.isEmpty(), getErrorMessage(errorMessages));
    }

    private static String getErrorMessage(List<String> errorMessages) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nFollowing errors occurred during servantlets processing:");
        for (int i = 0; i < errorMessages.size(); i++) {
            sb.append("\n").append(i + 1).append(". ");
            sb.append(errorMessages.get(i));
        }
        return sb.toString();
    }
}
