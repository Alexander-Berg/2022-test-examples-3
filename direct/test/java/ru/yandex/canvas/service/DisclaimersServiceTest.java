package ru.yandex.canvas.service;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import ru.yandex.canvas.model.disclaimers.Disclaimer;
import ru.yandex.canvas.model.disclaimers.DisclaimerDescription;
import ru.yandex.canvas.model.disclaimers.DisclaimerText;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

/**
 * @author skirsanov
 */
public class DisclaimersServiceTest {

    @Test
    public void testIds() {
        final DisclaimersService service = new DisclaimersService();

        final List<Disclaimer> disclaimers = service.getList();

        assertEquals(disclaimers.size(), disclaimers.stream().mapToInt(Disclaimer::getId).distinct().count());

        final List<DisclaimerDescription> descriptions = disclaimers.stream().map(Disclaimer::getDescriptions)
                .flatMap(Collection::stream).collect(toList());

        assertEquals(descriptions.size(), descriptions.stream().mapToInt(DisclaimerDescription::getId)
                .distinct().count());

        final List<DisclaimerText> texts = descriptions.stream().map(DisclaimerDescription::getTexts)
                .flatMap(Collection::stream).collect(toList());

        assertEquals(texts.size(), texts.stream().mapToInt(DisclaimerText::getId)
                .distinct().count());
    }
}
