package ru.yandex.ir.common.features.relevance.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.ir.common.string.SimpleStringProcessor;

import java.util.Map;

class IdfBuilderTest {

    @Test
    void calculateWordWeights() {
        SimpleStringProcessor stringProcessor = new SimpleStringProcessor();

        IdfBuilder builder = new IdfBuilder(stringProcessor);

        // ---

        builder.addDocument("Model 1 One", "a lot of not unique -");
        builder.addDocument("Model 2 Two", "a lot of not - words");
        builder.addDocument("Model 3 Three", "a lot of - unique words");
        builder.addDocument("Model 4 Four", "a lot - not unique words");
        builder.addDocument("Model 5 Five", "a - of not unique words");
        builder.addDocument("Model 6 Six", "- lot of not unique words");
        builder.addDocument("Model 7 Seven", "a lot of not unique -");
        builder.addDocument("Model 8 Eight", "a lot of not - words");
        builder.addDocument("Model 9 Nine", "a lot of - unique words");
        builder.addDocument("Model 10 One", "a lot - not unique words");
        builder.addDocument("Model 20 Two", "a - of not unique words");
        builder.addDocument("Model 30 Three", "- lot of not unique words");
        builder.addDocument("Model 40 Four", "a lot of not unique -");
        builder.addDocument("Model 50 Five", "a lot of not - words");
        builder.addDocument("Model 60 Six", "a lot of - unique words");
        builder.addDocument("Model 70 Seven", "a lot - not unique words");
        builder.addDocument("Model 80 Eight", "a - of not unique words");
        builder.addDocument("Model 90 Nine", "- lot of not unique words");
        builder.addDocument("Model 100 One", "a lot of not unique -");
        builder.addDocument("Model 200 Two", "a lot of not - words");
        builder.addDocument("Model 300 Three", "a lot of - unique words");
        builder.addDocument("Model 400 Four", "a lot - not unique words");
        builder.addDocument("Model 500 Five", "a - of not unique words");
        builder.addDocument("Model 600 Six", "- lot of not unique words");
        builder.addDocument("Model 700 Seven", "a lot of not unique -");
        builder.addDocument("Model 800 Eight", "a lot of not - words");
        builder.addDocument("Model 900 Nine", "a lot of - unique words");
        builder.addDocument("Model 1000 One", "a lot - not unique words");
        builder.addDocument("Model 2000 Two", "a - of not unique words");
        builder.addDocument("Model 3000 Three", "- lot of not unique words");
        builder.addDocument("Model 4000 Four", "a lot of not unique -");
        builder.addDocument("Model 5000 Five", "a lot of not - words");
        builder.addDocument("Model 6000 Six", "a lot of - unique words");
        builder.addDocument("Model 7000 Seven", "a lot - not unique words");
        builder.addDocument("Model 8000 Eight", "a - of not unique words");
        builder.addDocument("Model 9000 Nine", "- lot of not unique words");

        Map<String, IdfData> idfData = builder.build();

        // ---

        Assertions.assertEquals(0, idfData.get("model").getWeight(), 1e-100);
        Assertions.assertTrue(idfData.get("seven").getWeight() < idfData.get("7").getWeight());
        Assertions.assertTrue(idfData.get("words").getWeight() < idfData.get("seven").getWeight());
        Assertions.assertEquals(idfData.get("4").getWeight(), idfData.get("9").getWeight());
        Assertions.assertEquals(idfData.get("two").getWeight(), idfData.get("eight").getWeight());
    }
}
