package ru.yandex.canvas.steps;

import java.util.ArrayList;

import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.addition.AdditionData;
import ru.yandex.canvas.model.video.addition.AdditionDataBundle;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.Options;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;
import ru.yandex.canvas.model.video.addition.options.BodyElementOptions;
import ru.yandex.canvas.model.video.addition.options.ButtonElementOptions;

public class AdditionsSteps {
    public static final String DEFAULT_VIDEO_ID = "old_114";

    public static String defaultVideoId() {
        return DEFAULT_VIDEO_ID;
    }

    public static BodyElementOptions defaultBodyElementOptionsWithText() {
        return new BodyElementOptions()
                .setText("Some body I used to know")
                .setBackgroundColor("#0FCD18")
                .setTextColor("#121314");
    }

    public static BodyElementOptions defaultBodyElementOptions() {
        return new BodyElementOptions()
                .setBackgroundColor("#0FCD18")
                .setTextColor("#121314");
    }

    public static ButtonElementOptions defaultButtonElementOptions() {
        return new ButtonElementOptions()
                .setColor("#FF00FF")
                .setBorderColor("#CCFFDD")
                .setTextColor("#454543");
    }

    /**
     * smallest addition which is valid for least VideoPreset
     */
    public static Addition leastAddition(Long clientId, Long presetId, String videoId, AdditionElement... elementsToAdd) {
        AdditionDataBundle bundle = new AdditionDataBundle();
        bundle.setName("myBundle");

        Options additionElementOptions = new AdditionElementOptions()
                .setVideoId(videoId)
                .setAudioId(null);

        AdditionElement additionElement = new AdditionElement(AdditionElement.ElementType.ADDITION)
                .withOptions(additionElementOptions).withAvailable(true);

        ArrayList<AdditionElement> elements = new ArrayList<>();
        elements.add(additionElement);
        for (AdditionElement e : elementsToAdd) {
            e.withAvailable(true);
            elements.add(e);
        }
        AdditionData additionData = new AdditionData()
                .setBundle(bundle)
                .setElements(elements);

        Addition addition = new Addition();

        addition.setPresetId(presetId)
                .setClientId(clientId)
                .setData(additionData);

        return addition;
    }
}
