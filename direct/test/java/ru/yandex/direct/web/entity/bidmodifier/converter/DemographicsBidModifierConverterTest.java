package ru.yandex.direct.web.entity.bidmodifier.converter;

import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsBidModifier;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.entity.bidmodifier.converter.DemographicsBidModifierConverter.webDemographicsBidModifierToCore;

public class DemographicsBidModifierConverterTest {

    @Test
    public void ageAllIsConvertedToNull() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier("all", 123);
        BidModifierDemographics coreModifier = webDemographicsBidModifierToCore(webModifier);
        assertThat(coreModifier.getDemographicsAdjustments().get(0).getAge(), nullValue());
    }

    @Test
    public void ageNullIsConvertedToNull() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier(null, 123);
        BidModifierDemographics coreModifier = webDemographicsBidModifierToCore(webModifier);
        assertThat(coreModifier.getDemographicsAdjustments().get(0).getAge(), nullValue());
    }

    @Test(expected = IllegalStateException.class)
    public void unexpectedAgeThrowsException() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier("1-2", 123);
        webDemographicsBidModifierToCore(webModifier);
    }

    @Test
    public void genderNullIsConvertedToNull() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier(null, null, 123);
        BidModifierDemographics coreModifier = webDemographicsBidModifierToCore(webModifier);
        assertThat(coreModifier.getDemographicsAdjustments().get(0).getGender(), nullValue());
    }

    @Test
    public void genderAllIsConvertedToNull() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier(null, "all", 123);
        BidModifierDemographics coreModifier = webDemographicsBidModifierToCore(webModifier);
        assertThat(coreModifier.getDemographicsAdjustments().get(0).getGender(), nullValue());
    }

    @Test
    public void genderMaleIsConvertedToMale() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier(null, "male", 123);
        BidModifierDemographics coreModifier = webDemographicsBidModifierToCore(webModifier);
        assertThat(coreModifier.getDemographicsAdjustments().get(0).getGender(), is(GenderType.MALE));
    }

    @Test
    public void genderFemaleIsConvertedToFemale() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier(null, "female", 123);
        BidModifierDemographics coreModifier = webDemographicsBidModifierToCore(webModifier);
        assertThat(coreModifier.getDemographicsAdjustments().get(0).getGender(), is(GenderType.FEMALE));
    }

    @Test(expected = IllegalStateException.class)
    public void unexpectedGenderThrowsException() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier(null, "strange", 123);
        webDemographicsBidModifierToCore(webModifier);
    }

    private static WebDemographicsBidModifier singleDemographicsBidModifier(String age, Integer percent) {
        return new WebDemographicsBidModifier()
                .withEnabled(1)
                .withAdjustments(singletonList(demographicsAdjustment(age, percent)));
    }

    private static WebDemographicsBidModifier singleDemographicsBidModifier(
            String age, String gender, Integer percent) {
        return new WebDemographicsBidModifier()
                .withEnabled(1)
                .withAdjustments(singletonList(demographicsAdjustment(age, gender, percent)));
    }

    private static WebDemographicsAdjustment demographicsAdjustment(String age, Integer percent) {
        return demographicsAdjustment(age, null, percent);
    }

    private static WebDemographicsAdjustment demographicsAdjustment(String age, String gender, Integer percent) {
        return (WebDemographicsAdjustment) new WebDemographicsAdjustment()
                .withAge(age)
                .withGender(gender)
                .withPercent(percent);
    }
}
