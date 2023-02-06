package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.errors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.front.errorBooster.LogLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorsContainerTest {

    @Test
    public void prepareMessage() {
        ErrorsContainer errorsContainer = new ErrorsContainer();

        assertEquals("Script error", errorsContainer.prepareMessage("Script error"));
        assertEquals("Script error", errorsContainer.prepareMessage("Script error."));

        assertEquals(
            "TypeError: a.clear is not a function",
            errorsContainer.prepareMessage("Uncaught TypeError: a.clear is not a function")
        );

        assertEquals(
            "TypeError: 'undefined' is not a function",
            errorsContainer.prepareMessage("TypeError: 'undefined' is not a function")
        );

        assertEquals(
            "URIError: URI malformed",
            errorsContainer.prepareMessage("Uncaught URIError: URI malformed")
        );

        assertEquals(
            "first url '{{REPLACED_MESSAGE_URL_0}}' second url \"{{REPLACED_MESSAGE_URL_1}}\"",
            errorsContainer.prepareMessage("first url 'https://yandex.ru/' second url \"https://yastatic.net/\"")
        );

        assertEquals(
            "TypeError: Object {{REPLACED_MESSAGE_URL_0}} has no method 'click'",
            errorsContainer.prepareMessage("Uncaught TypeError: Object https://market-click2.yandex" +
                ".ru/redir" +
                "/GAkkM7lQwz62j9BQ6_qgZowZt7ZtTlkMgnn1pYLu7y6WNWjjb_Ot98kVDBborDY87AX9xgfSJDhjDeNFMmBn8SIyPF6fSzQp3A" +
                "VbQe0-CghFrGYigDgy_EgwSameD8zv3eLxFyJUUpbQWE_YahercBo2AZTS-_FTtIsFTA3v7ohjvjJf7IWReXDMeSf9JA24ocv0u" +
                "8ne7abokjJaFb1P4_SMIVO81T769VNoYOFxAryrZ-t9wgk48pvlZljLV_pPRrZ1n2YsKLAWGQuo8RlcITo1j2Z0CSB_ErhHTTP8" +
                "4_16fQarPZPpUX2HNcWrSGrs6guwTemmUJngt5ZzeXvE1b6H0u8gOah9q9PCKJuBgI2E-VXl2Nw6W1JXPwHsRES5SwsWeGLuBdz" +
                "1qumZtzkGrUL75nDy5wWU0b_bqXh4Px8hmjvEin0dazz1h83M4HMqsipYHY2hHXZ9yTtbgjak3bQKKncSVRyD8d0IBSsWbx9x1k" +
                "S7rSxhRhBN9D_uUKsa3EMDsaRD03etK5sivQBJn3XSwAku9CMx5gkF0w24NqSzxlLVqSwj9eCEn6YWqw8loBCncevLMEyqJZNoU" +
                "5TknZ66EsbGtEKresRt7tz0KnKHxIWHjM0drzkQGNbhhF6eihAeI2gARSZWtDVxtVH7T-xqA2AgQMkKwXwnKUKJ_rYZCSeaCluO" +
                "av9lPzQyL5AD5TSxXdXLYIc1Seh8Jbn0mUWrUjU5u9_e8ulDL_eSsjeF_6gniX5FKbHobqwg6TEvzyy_dcvhtQ_x9V6j-KAB-cr" +
                "7S6baLJ3FUR_t-yJfG4jrP32GWNwPPU9K2Q-jOMUw0UWxOu0V--tsUJDIeIDjkMzN28e4do6NlYl3VKXu-7XAzi5wgsxT3Mx1LJ" +
                "1Or43uVSe3ILWjDoGRb00QRQY2mKu545DgW_f-eYAhJBeDRv5uOEFdDKob_fJYHfRLFAfoRQHPoF6E3P " +
                "has no method 'click'")
        );

        assertEquals(
            "A history state object with URL '{{REPLACED_MESSAGE_URL_0}}' cannot be created in a document with origin" +
                " '{{REPLACED_MESSAGE_URL_1}}'",
            errorsContainer.prepareMessage("A history state object with URL 'https://week-news24" +
                ".ru/main?utm_source=direct&utm_content=vit-lek&utm_medium=888&utm_term=rkbuy&utm_campaign" +
                "=5801141935c6ee7f2efb65&sid1=[TEASER_ID]&sid2=main_t&sid3=&sid4=main_t&sid5=&lr=236' cannot be " +
                "created in a document with origin 'https://yandex.ru/'")
        );

        assertEquals(
            "{{REPLACED_MESSAGE_URL_0}} == {{REPLACED_MESSAGE_URL_0}}",
            errorsContainer.prepareMessage("https://yandex.ru/ == https://yandex.ru/")
        );

        assertEquals(
            "{{REPLACED_MESSAGE_URL_0}} == {{REPLACED_MESSAGE_URL_0}}",
            errorsContainer.prepareMessage("https://yandex.ru == https://yandex.ru")
        );

        assertEquals(
            "TypeError: Cannot read property '{{REPLACED_MESSAGE_URL_0}}' of undefined",
            errorsContainer.prepareMessage("Uncaught TypeError: Cannot read property '//yandex" +
                ".ru/suggest/suggest-endings?srv=morda_ru' of undefined")
        );

        assertEquals(
            "TypeError: Cannot read property '{{REPLACED_MESSAGE_URL_0}}' of undefined",
            errorsContainer.prepareMessage("Uncaught TypeError: Cannot read property '//yandex.ru' of undefined")
        );

        assertEquals(
            "// комменты",
            errorsContainer.prepareMessage("// комменты")
        );

        assertEquals(
            "//комменты",
            errorsContainer.prepareMessage("//комменты")
        );

        assertEquals(
            "{{REPLACED_MESSAGE_URL_0}}",
            errorsContainer.prepareMessage("//localhost")
        );

        assertEquals(
            "{{REPLACED_MESSAGE_URL_0}}",
            errorsContainer.prepareMessage("//LOCALHOST")
        );

        assertEquals(
            "{{REPLACED_MESSAGE_URL_0}}",
            errorsContainer.prepareMessage("//09.ru")
        );
    }

    @Test
    public void prepareStackTrace() {
        ErrorsContainer errorsContainer = new ErrorsContainer();

        assertEquals(
            "Error: Mouse tracking requires global $ and BEM, or Ya.define\n" +
                "    at {{REPLACED_STACKTRACE_URL_0}}",
            errorsContainer.prepareStacktrace("Error: Mouse tracking requires global $ and BEM, or Ya.define\n" +
                "    at https://yastatic.net/s3/web4static/_/FzXy8SAGSQRgeJIgZ-AasncGkJ8.js:1:8186")
        );

        assertEquals(
            "getPlatformTests/t<@{{REPLACED_STACKTRACE_URL_0}}\n" +
                "detectAdblockType@{{REPLACED_STACKTRACE_URL_1}}\n" +
                "launch@{{REPLACED_STACKTRACE_URL_2}}\n" +
                "inited@{{REPLACED_STACKTRACE_URL_3}}\n" +
                "_callModFn@{{REPLACED_STACKTRACE_URL_4}}\n" +
                "setMod/<@{{REPLACED_STACKTRACE_URL_5}}\n" +
                "setMod@{{REPLACED_STACKTRACE_URL_6}}\n" +
                "setMod@{{REPLACED_STACKTRACE_URL_7}}\n" +
                "o@{{REPLACED_STACKTRACE_URL_8}}\n" +
                "_init@{{REPLACED_STACKTRACE_URL_9}}\n" +
                "_init@{{REPLACED_STACKTRACE_URL_10}}\n" +
                "o@{{REPLACED_STACKTRACE_URL_8}}\n" +
                "__constructor/<@{{REPLACED_STACKTRACE_URL_12}}\n" +
                "_runAfterCurrentEventFns/<@{{REPLACED_STACKTRACE_URL_13}}\n" +
                "_runAfterCurrentEventFns@{{REPLACED_STACKTRACE_URL_14}}\n" +
                "init@{{REPLACED_STACKTRACE_URL_15}}\n" +
                "t@{{REPLACED_STACKTRACE_URL_16}}\n" +
                "_runAfterCurrentEventFns/<@{{REPLACED_STACKTRACE_URL_13}}\n" +
                "_runAfterCurrentEventFns@{{REPLACED_STACKTRACE_URL_14}}",
            errorsContainer.prepareStacktrace("getPlatformTests/t<@https://yastatic" +
                ".net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:240338\n" +
                "detectAdblockType@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:235759\n" +
                "launch@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:235404\n" +
                "inited@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:235339\n" +
                "_callModFn@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:8025\n" +
                "setMod/<@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:7352\n" +
                "setMod@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:7330\n" +
                "setMod@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:16807\n" +
                "o@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:1075\n" +
                "_init@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:5429\n" +
                "_init@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:10350\n" +
                "o@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:1075\n" +
                "__constructor/<@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:5237\n" +
                "_runAfterCurrentEventFns/<@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc" +
                ".js:3:9914\n" +
                "_runAfterCurrentEventFns@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc" +
                ".js:3:9894\n" +
                "init@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:19941\n" +
                "t@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:91392\n" +
                "_runAfterCurrentEventFns/<@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc" +
                ".js:3:9914\n" +
                "_runAfterCurrentEventFns@https://yastatic.net/s3/web4static/_/0qdnv5fYRjWSUThJ7HGRBcEuKdc.js:3:9894")
        );
    }

    @Test
    public void prepareLevel() {
        ErrorsContainer errorsContainer = new ErrorsContainer();
        assertEquals(LogLevel.UNKNOWN, errorsContainer.prepareLevel("", PathError.CLIENT));
        assertEquals(LogLevel.UNKNOWN, errorsContainer.prepareLevel("", PathError.NODEJS));
        assertEquals(LogLevel.ERROR, errorsContainer.prepareLevel("", PathError.EXTERNAL));
        assertEquals(LogLevel.ERROR, errorsContainer.prepareLevel("", PathError.SCRIPT));
        assertEquals(LogLevel.ERROR, errorsContainer.prepareLevel("", PathError.UNCAUGHT));
        assertEquals(LogLevel.WARNING, errorsContainer.prepareLevel("warn", PathError.CLIENT));
        assertEquals(LogLevel.WARNING, errorsContainer.prepareLevel("warn", PathError.EXTERNAL));
        assertEquals(LogLevel.WARNING, errorsContainer.prepareLevel("warn", PathError.NODEJS));
        assertEquals(LogLevel.WARNING, errorsContainer.prepareLevel("warn", PathError.SCRIPT));
        assertEquals(LogLevel.WARNING, errorsContainer.prepareLevel("warn", PathError.UNCAUGHT));
        assertEquals(LogLevel.WARNING, errorsContainer.prepareLevel("warn", PathError.UNKNOWN));
        assertEquals(LogLevel.UNKNOWN, errorsContainer.prepareLevel("bad", PathError.UNKNOWN));
    }
}
