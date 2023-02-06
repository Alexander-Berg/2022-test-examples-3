package ru.yandex.canvas.service.video;

import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import ru.yandex.canvas.TimeDelta;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;
import ru.yandex.canvas.model.video.addition.options.BodyElementOptions;
import ru.yandex.canvas.model.video.addition.options.ButtonElementOptions;
import ru.yandex.canvas.model.video.addition.options.DomainElementOptions;

import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertEquals;

public class AdParamsTest {

    @Test
    public void createJsonSmokeTest()
            throws JsonProcessingException {

        String expected = "{\n"
                + "  \"HAS_BODY\" : true,\n"
                + "  \"BODY_BACKGROUND_COLOR\" : \"#baba00\",\n"
                + "  \"BODY_TEXT_COLOR\" : \"#0c0c0c\",\n"
                + "  \"BODY_TEXT\" : \"my text\",\n"
                + "  \"HAS_DOMAIN\" : false,\n"
                + "  \"HAS_BUTTON\" : false,\n"
                + "  \"HAS_TITLE\" : false,\n"
                + "  \"HAS_AGE\" : false,\n"
                + "  \"theme\" : \"caucasus theme\",\n"
                + "  \"duration\" : 5.0,\n"
                + "  \"mediaFiles\" : ${MEDIA_FILES},\n"
                + "  \"AUCTION_DC_PARAMS\" : ${AUCTION_DC_PARAMS},\n"
                + "  \"playbackParameters\" : {\n"
                + "    \"showSkipButton\" : true,\n"
                + "    \"skipDelay\" : 65\n"
                + "  },\n"
                + "  \"encounters\" : [ \"${TRACKING_URL_PREFIX}?action-id=14\" ]\n"
                + "}";


        AdditionElementOptions additionOptions = new AdditionElementOptions();
        additionOptions.setAudioId("AUDIO_ID");
        additionOptions.setVideoId("VIDEO_ID");

        BodyElementOptions bodyOptions = new BodyElementOptions();
        bodyOptions.setTextColor("#0c0c0c");
        bodyOptions.setText("my text");
        bodyOptions.setBackgroundColor("#baba00");

        AdditionElement addition =
                new AdditionElement(AdditionElement.ElementType.ADDITION).withOptions(additionOptions)
                        .withAvailable(true);
        AdditionElement body =
                new AdditionElement(AdditionElement.ElementType.BODY).withOptions(bodyOptions).withAvailable(true);

        String json = new AdParams()
                .setDuration(5.0)
                .setElements(Arrays.asList(addition, body))
                .setTheme("caucasus theme")
                .setSkipDelay(new TimeDelta(65))
                .toJson();

        assertEquals("Json as expected", expected, json);
    }

    @Test
    public void createJsonWithUnavailableBodyTest()
            throws JsonProcessingException {

        String expected = "{\n"
                + "  \"HAS_DOMAIN\" : false,\n"
                + "  \"HAS_BUTTON\" : false,\n"
                + "  \"HAS_TITLE\" : false,\n"
                + "  \"HAS_BODY\" : false,\n"
                + "  \"HAS_AGE\" : false,\n"
                + "  \"theme\" : \"caucasus theme\",\n"
                + "  \"duration\" : 5.0,\n"
                + "  \"mediaFiles\" : ${MEDIA_FILES},\n"
                + "  \"AUCTION_DC_PARAMS\" : ${AUCTION_DC_PARAMS},\n"
                + "  \"playbackParameters\" : {\n"
                + "    \"showSkipButton\" : true,\n"
                + "    \"skipDelay\" : 65\n"
                + "  },\n"
                + "  \"encounters\" : [ \"${TRACKING_URL_PREFIX}?action-id=14\" ]\n"
                + "}";


        AdditionElementOptions additionOptions = new AdditionElementOptions();
        additionOptions.setAudioId("AUDIO_ID");
        additionOptions.setVideoId("VIDEO_ID");

        BodyElementOptions bodyOptions = new BodyElementOptions();
        bodyOptions.setTextColor("#0c0c0c");
        bodyOptions.setText("my text");
        bodyOptions.setBackgroundColor("#baba00");

        AdditionElement addition =
                new AdditionElement(AdditionElement.ElementType.ADDITION).withOptions(additionOptions)
                        .withAvailable(true);
        AdditionElement body =
                new AdditionElement(AdditionElement.ElementType.BODY).withOptions(bodyOptions).withAvailable(false);

        String json = new AdParams()
                .setDuration(5.0)
                .setElements(Arrays.asList(addition, body))
                .setTheme("caucasus theme")
                .setSkipDelay(new TimeDelta(65))
                .toJson();

        assertEquals("Json as expected", expected, json);
    }

    @Test
    public void createJsonWithButtonAndDomainTest()
            throws JsonProcessingException {

        String expected = "{\n"
                + "  \"HAS_DOMAIN\" : true,\n"
                + "  \"DOMAIN_BACKGROUND_COLOR\" : \"#abcdff\",\n"
                + "  \"DOMAIN_TEXT_COLOR\" : \"#0cabcd\",\n"
                + "  \"DOMAIN_TEXT\" : \"http://yandex.ru\",\n"
                + "  \"HAS_BUTTON\" : true,\n"
                + "  \"BUTTON_TEXT_COLOR\" : \"#0c0c0c\",\n"
                + "  \"BUTTON_TEXT\" : \"my text\",\n"
                + "  \"BUTTON_BORDER_COLOR\" : \"\",\n"
                + "  \"BUTTON_COLOR\" : \"#babacc\",\n"
                + "  \"HAS_TITLE\" : false,\n"
                + "  \"HAS_BODY\" : false,\n"
                + "  \"HAS_AGE\" : false,\n"
                + "  \"theme\" : \"caucasus theme\",\n"
                + "  \"duration\" : 5.0,\n"
                + "  \"mediaFiles\" : ${MEDIA_FILES},\n"
                + "  \"AUCTION_DC_PARAMS\" : ${AUCTION_DC_PARAMS},\n"
                + "  \"playbackParameters\" : {\n"
                + "    \"showSkipButton\" : true,\n"
                + "    \"skipDelay\" : 65\n"
                + "  },\n"
                + "  \"encounters\" : [ \"${TRACKING_URL_PREFIX}?action-id=14\" ]\n"
                + "}";


        AdditionElementOptions additionOptions = new AdditionElementOptions();
        additionOptions.setAudioId("AUDIO_ID");
        additionOptions.setVideoId("VIDEO_ID");

        ButtonElementOptions buttonElementOptions = new ButtonElementOptions();
        buttonElementOptions.setTextColor("#0c0c0c");
        buttonElementOptions.setText("my text");
        buttonElementOptions.setColor("#babacc");
        buttonElementOptions.setBorderColor("#baba00");

        DomainElementOptions domainElementOptions = new DomainElementOptions();
        domainElementOptions.setTextColor("#0cabcd")
                .setText("http://yandex.ru").setBackgroundColor("#abcdff");


        AdditionElement domain = new AdditionElement(AdditionElement.ElementType.DOMAIN).withOptions(domainElementOptions)
                .withAvailable(true);

        AdditionElement addition =
                new AdditionElement(AdditionElement.ElementType.ADDITION).withOptions(additionOptions)
                        .withAvailable(true);
        AdditionElement body =
                new AdditionElement(AdditionElement.ElementType.BUTTON).withOptions(buttonElementOptions).withAvailable(true);

        String json = new AdParams()
                .setDuration(5.0)
                .setElements(Arrays.asList(domain, addition, body))
                .setTheme("caucasus theme")
                .setSkipDelay(new TimeDelta(65))
                .toJson();

        assertEquals("Json as expected", expected, json);
    }

    @Test
    public void createJsonWithoutElementsTest()
            throws JsonProcessingException {
        String expected = "{\n"
                + "  \"theme\" : \"caucasus theme\",\n"
                + "  \"duration\" : 5.0,\n"
                + "  \"mediaFiles\" : ${MEDIA_FILES},\n"
                + "  \"AUCTION_DC_PARAMS\" : ${AUCTION_DC_PARAMS},\n"
                + "  \"playbackParameters\" : {\n"
                + "    \"showSkipButton\" : true,\n"
                + "    \"skipDelay\" : 65\n"
                + "  },\n"
                + "  \"encounters\" : [ \"${TRACKING_URL_PREFIX}?action-id=14\" ]\n"
                + "}";

        String json = new AdParams()
                .setDuration(5.0)
                .setElements(null)
                .setTheme("caucasus theme")
                .setSkipDelay(new TimeDelta(65))
                .toJson();

        assertEquals("Json as expected", expected, json);
    }

    @Test
    public void createJsonWithEmptyElementsTest()
            throws JsonProcessingException {
        String expected = "{\n"
                + "  \"HAS_DOMAIN\" : false,\n"
                + "  \"HAS_BUTTON\" : false,\n"
                + "  \"HAS_TITLE\" : false,\n"
                + "  \"HAS_BODY\" : false,\n"
                + "  \"HAS_AGE\" : false,\n"
                + "  \"theme\" : \"caucasus theme\",\n"
                + "  \"duration\" : 5.0,\n"
                + "  \"mediaFiles\" : ${MEDIA_FILES},\n"
                + "  \"AUCTION_DC_PARAMS\" : ${AUCTION_DC_PARAMS},\n"
                + "  \"playbackParameters\" : {\n"
                + "    \"showSkipButton\" : true,\n"
                + "    \"skipDelay\" : 65\n"
                + "  },\n"
                + "  \"encounters\" : [ \"${TRACKING_URL_PREFIX}?action-id=14\" ]\n"
                + "}";

        String json = new AdParams()
                .setDuration(5.0)
                .setElements(emptyList())
                .setTheme("caucasus theme")
                .setSkipDelay(new TimeDelta(65))
                .toJson();

        assertEquals("Json as expected", expected, json);
    }

    @Test
    public void createJsonWithPackshot()
            throws JsonProcessingException {

        String expected = "{\n"
                + "  \"HAS_BODY\" : true,\n"
                + "  \"BODY_BACKGROUND_COLOR\" : \"#baba00\",\n"
                + "  \"BODY_TEXT_COLOR\" : \"#0c0c0c\",\n"
                + "  \"BODY_TEXT\" : \"my text\",\n"
                + "  \"HAS_DOMAIN\" : false,\n"
                + "  \"HAS_BUTTON\" : false,\n"
                + "  \"HAS_TITLE\" : false,\n"
                + "  \"HAS_AGE\" : false,\n"
                + "  \"theme\" : \"caucasus theme\",\n"
                + "  \"duration\" : 4.2,\n"
                + "  \"mediaFiles\" : ${MEDIA_FILES},\n"
                + "  \"packshot_duration\" : 2.8,\n"
                + "  \"PACKSHOT_START_NOTICE_URL\" : \"${TRACKING_URL_PREFIX}?action-id=10\",\n"
                + "  \"PACKSHOT_IMAGE_URL\" : \"http://images.com/mypackshot/\",\n"
                + "  \"AUCTION_DC_PARAMS\" : ${AUCTION_DC_PARAMS},\n"
                + "  \"playbackParameters\" : {\n"
                + "    \"showSkipButton\" : true,\n"
                + "    \"skipDelay\" : 65\n"
                + "  },\n"
                + "  \"encounters\" : [ \"${TRACKING_URL_PREFIX}?action-id=14\" ]\n"
                + "}";


        AdditionElementOptions additionOptions = new AdditionElementOptions();
        additionOptions.setAudioId("AUDIO_ID");
        additionOptions.setVideoId("VIDEO_ID");

        BodyElementOptions bodyOptions = new BodyElementOptions();
        bodyOptions.setTextColor("#0c0c0c");
        bodyOptions.setText("my text");
        bodyOptions.setBackgroundColor("#baba00");

        AdditionElement addition =
                new AdditionElement(AdditionElement.ElementType.ADDITION).withOptions(additionOptions)
                        .withAvailable(true);
        AdditionElement body =
                new AdditionElement(AdditionElement.ElementType.BODY).withOptions(bodyOptions).withAvailable(true);

        String json = new AdParams()
                .setDuration(4.2)
                .setElements(Arrays.asList(addition, body))
                .setTheme("caucasus theme")
                .setPackshotImageUrl("http://images.com/mypackshot/")
                .setSkipDelay(new TimeDelta(65))
                .toJson();

        assertEquals("Json as expected", expected, json);
    }

    @Test
    public void createJsonWithJSIncompatibleBody()
            throws JsonProcessingException {

        String expected = "{\n"
                + "  \"HAS_BODY\" : true,\n"
                + "  \"BODY_BACKGROUND_COLOR\" : \"#baba00\",\n"
                + "  \"BODY_TEXT_COLOR\" : \"#0c0c0c\",\n"
                + "  \"BODY_TEXT\" : \"ПРИВЕТ МИР! \\u0000\\u00ad\\u0600-\\u0604\\u070f\\u17b4\\u17b5-\\u200f\\u2028-\\u202f\\u2060-\\u206f\\ufff0-\\uffff\",\n"
                + "  \"HAS_DOMAIN\" : false,\n"
                + "  \"HAS_BUTTON\" : false,\n"
                + "  \"HAS_TITLE\" : false,\n"
                + "  \"HAS_AGE\" : false,\n"
                + "  \"theme\" : \"caucasus theme\",\n"
                + "  \"duration\" : 4.2,\n"
                + "  \"mediaFiles\" : ${MEDIA_FILES},\n"
                + "  \"packshot_duration\" : 2.8,\n"
                + "  \"PACKSHOT_START_NOTICE_URL\" : \"${TRACKING_URL_PREFIX}?action-id=10\",\n"
                + "  \"PACKSHOT_IMAGE_URL\" : \"http://images.com/mypackshot/\",\n"
                + "  \"AUCTION_DC_PARAMS\" : ${AUCTION_DC_PARAMS},\n"
                + "  \"playbackParameters\" : {\n"
                + "    \"showSkipButton\" : true,\n"
                + "    \"skipDelay\" : 65\n"
                + "  },\n"
                + "  \"encounters\" : [ \"${TRACKING_URL_PREFIX}?action-id=14\" ]\n"
                + "}";

        AdditionElementOptions additionOptions = new AdditionElementOptions();
        additionOptions.setAudioId("AUDIO_ID");
        additionOptions.setVideoId("VIDEO_ID");

        BodyElementOptions bodyOptions = new BodyElementOptions();
        bodyOptions.setTextColor("#0c0c0c");

        //Owls aren't what they are look like (invisible symbols!)
        bodyOptions.setText("ПРИВЕТ МИР! \u0000\u00AD\u0600-\u0604\u070F឴឵-\u200F\u2028- \u2060-\u206F\uFFF0-\uFFFF");
        bodyOptions.setBackgroundColor("#baba00");

        AdditionElement addition =
                new AdditionElement(AdditionElement.ElementType.ADDITION).withOptions(additionOptions)
                        .withAvailable(true);
        AdditionElement body =
                new AdditionElement(AdditionElement.ElementType.BODY).withOptions(bodyOptions).withAvailable(true);

        String json = new AdParams()
                .setDuration(4.2)
                .setElements(Arrays.asList(addition, body))
                .setTheme("caucasus theme")
                .setPackshotImageUrl("http://images.com/mypackshot/")
                .setSkipDelay(new TimeDelta(65))
                .toJson();

        assertEquals("Json as expected", expected, json);
    }
}
