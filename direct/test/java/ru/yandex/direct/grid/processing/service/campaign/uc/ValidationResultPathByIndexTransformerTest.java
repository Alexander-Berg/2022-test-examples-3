package ru.yandex.direct.grid.processing.service.campaign.uc;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.validation.KeywordsAddValidationService;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.validation.result.PathNode;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public final class ValidationResultPathByIndexTransformerTest {
    @Autowired
    private KeywordsAddValidationService keywordsAddValidationService;

    @Test
    public void changeExistIndexes() {
        final var firstKeyword = new Keyword().withPhrase("конь");
        final var secondKeyword = new Keyword().withPhrase("-як");
        final var vr = keywordsAddValidationService.preValidate(List.of(firstKeyword, secondKeyword), false);
        final var result = vr.transform(new ValidationResultPathByIndexTransformer(Map.of(0, 2, 1, 3)));

        Assert.assertEquals(result.getSubResults().size(), 2);
        Assert.assertTrue(result.getSubResults().containsKey(new PathNode.Index(2)));
        Assert.assertTrue(result.getSubResults().containsKey(new PathNode.Index(3)));
    }
}
