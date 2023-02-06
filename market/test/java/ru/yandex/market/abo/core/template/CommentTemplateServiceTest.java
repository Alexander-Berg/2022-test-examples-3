package ru.yandex.market.abo.core.template;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.template.model.CommentTemplate;
import ru.yandex.market.abo.core.template.model.TemplateEntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author agavrikov
 * @date 20.11.18
 */
public class CommentTemplateServiceTest extends EmptyTest {

    @Autowired
    private CommentTemplateService service;

    @Test
    public void loadTest() {
        CommentTemplate template1 = new CommentTemplate();
        template1.setEntityId(1);
        template1.setEntityType(TemplateEntityType.CORE_PROBLEM);
        template1.setText("text1");
        service.save(template1);

        CommentTemplate template2 = new CommentTemplate();
        template2.setEntityId(2);
        template2.setEntityType(TemplateEntityType.ARBITRAGE);
        template2.setText("text2");
        service.save(template2);

        List<CommentTemplate> arbitrageTemplateList = service.load(TemplateEntityType.ARBITRAGE);
        assertTrue(arbitrageTemplateList.size() > 0);
        CommentTemplate arbitrageTemplate = arbitrageTemplateList.get(0);
        assertEquals(TemplateEntityType.ARBITRAGE, arbitrageTemplate.getEntityType());

        List<CommentTemplate> coreProblemTemplateList = service.load(TemplateEntityType.CORE_PROBLEM);
        assertTrue(coreProblemTemplateList.size() > 0);
        CommentTemplate coreProblemTemplate = coreProblemTemplateList.get(0);
        assertEquals(TemplateEntityType.CORE_PROBLEM, coreProblemTemplate.getEntityType());
    }

    @Test
    public void loadAndDeleteTest() {
        CommentTemplate template1 = new CommentTemplate();
        template1.setEntityId(1);
        template1.setEntityType(TemplateEntityType.CORE_PROBLEM);
        template1.setText("text1");
        service.save(template1);

        CommentTemplate dbTemplate = service.load(template1.getId());
        assertEquals(template1, dbTemplate);

        service.delete(template1.getId());

        assertNull(service.load(template1.getId()));
    }
}
