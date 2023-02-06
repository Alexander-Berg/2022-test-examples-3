package ru.yandex.market.ir.classifier.trainer.tasks.logic;

import org.junit.Test;
import ru.yandex.market.ir.classifier.model.HonestMarkDepartments;
import ru.yandex.market.ir.http.Classifier.HonestMarkDepartmentProbability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HonestMarkQualityCheckHelperTest {

    HonestMarkQualityCheckHelper honestMarkQualityCheckHelper = new HonestMarkQualityCheckHelper();

    @Test
    public void shouldFindTheMostPossibleDepartment() {
        List<HonestMarkDepartmentProbability> probabilities = Arrays.asList(
            HonestMarkDepartmentProbability.newBuilder()
                .setName(HonestMarkDepartments.Department.TIRES.getApiName())
                .setProbability(0.9700003)
                .build(),
            HonestMarkDepartmentProbability.newBuilder()
                .setName(HonestMarkDepartments.Department.BOOTS.getApiName())
                .setProbability(0.02)
                .build(),
            HonestMarkDepartmentProbability.newBuilder()
                .setName(HonestMarkDepartments.Department.OTHER.getApiName())
                .setProbability(0.0003)
                .build()
        );

        HonestMarkDepartments.Department mostPossibleDepartment =
            honestMarkQualityCheckHelper.getMostPossibleDepartment(probabilities);

        assertEquals(HonestMarkDepartments.Department.TIRES, mostPossibleDepartment);
    }

    @Test
    public void shouldReturnOtherIfDepartmentsProbabilitiesIsEmpty() {
        HonestMarkDepartments.Department mostPossibleDepartment =
            honestMarkQualityCheckHelper.getMostPossibleDepartment(new ArrayList<>());

        assertEquals(HonestMarkDepartments.Department.OTHER, mostPossibleDepartment);
    }
}