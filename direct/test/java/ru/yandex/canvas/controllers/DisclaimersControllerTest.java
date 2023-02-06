package ru.yandex.canvas.controllers;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.yandex.canvas.model.disclaimers.Disclaimer;
import ru.yandex.canvas.model.disclaimers.DisclaimerDescription;
import ru.yandex.canvas.model.disclaimers.DisclaimerText;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.canvas.controllers.DisclaimersController.mapToDTO;

/**
 * @author skirsanov
 */
public class DisclaimersControllerTest {

    @Test
    public void testDisclaimersMerge() {
        final List<Disclaimer> disclaimers = new ArrayList<>(2);

        disclaimers.add(new Disclaimer(1, "finance",
                singletonList(new DisclaimerDescription(1, "RUS", 643,
                        singletonList(new DisclaimerText(1, "do good"))))));

        disclaimers.add(new Disclaimer(2, "medicine",
                singletonList(new DisclaimerDescription(2, "RUS", 643,
                        singletonList(new DisclaimerText(2, "do not do evil"))))));


        DisclaimersController.DisclaimersDTO disclaimersDTO = mapToDTO(disclaimers);


        assertEquals("disclaimers with same countries must be merged into one",
                1, disclaimersDTO.getItems().size());

        DisclaimersController.DisclaimerDTO disclaimerDTO = disclaimersDTO.getItems().iterator().next();

        assertEquals("disclaimer texts from different disclaimers must be present in merged disclaimer DTO",
                2, disclaimerDTO.getItems().size());
    }
}
