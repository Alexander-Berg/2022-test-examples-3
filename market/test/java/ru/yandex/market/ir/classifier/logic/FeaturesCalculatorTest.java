package ru.yandex.market.ir.classifier.logic;

import java.util.Locale;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.ir.util.SimpleStringCache;
import ru.yandex.market.ir.classifier.dao.ClassifierDao;
import ru.yandex.market.ir.classifier.model.ClassifierParams;
import ru.yandex.market.ir.classifier.model.HttpClassificationRequest;
import ru.yandex.market.ir.classifier.model.PreparedRequest;
import ru.yandex.market.ir.http.Classifier;

import static org.junit.Assert.assertEquals;

public class FeaturesCalculatorTest {
    private static final Locale DEFAULT_LANGUAGE = Locale.forLanguageTag("RU");

    private FeaturesCalculator featuresCalculator;

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void setUp() {
        featuresCalculator = new FeaturesCalculator(applicationContext);
    }
    @Test
    public void whenEmptyTitleThenNoCandidates() {
        Configuration configuration = createDefaultConfiguration();
        HttpClassificationRequest httpClassificationRequest =
                HttpClassificationRequest.fromOffer(Classifier.Offer.newBuilder().build());
        PreparedRequest preparedRequest = new PreparedRequest(
                0,
                httpClassificationRequest,
                configuration,
                false,
                DEFAULT_LANGUAGE
        );
        IntSet probableCategories = featuresCalculator.getProbableCategories(preparedRequest);
        assertEquals(0, probableCategories.size());
    }

    @NotNull
    private Configuration createDefaultConfiguration() {
        Object2ObjectMap<String, IntSet> resultOfferTypes = new Object2ObjectLinkedOpenHashMap<>();
        resultOfferTypes.defaultReturnValue(IntSets.EMPTY_SET);
        ClassifierDao.OfferTypeRestrictionData offerTypeRestrictionData = new ClassifierDao.OfferTypeRestrictionData(
                resultOfferTypes,
                Object2IntMaps.EMPTY_MAP
        );

        Configuration configuration = new Configuration();
        configuration.setClassifierParams(new ClassifierParams(new SimpleStringCache(), DEFAULT_LANGUAGE));
        configuration.setOfferTypeRestrictionData(offerTypeRestrictionData);
        return configuration;
    }
}
