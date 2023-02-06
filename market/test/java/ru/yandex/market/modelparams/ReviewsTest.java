package ru.yandex.market.modelparams;

import junit.framework.TestCase;
import ru.yandex.market.modelparams.model.ModelReview;
import ru.yandex.market.modelparams.reviews.ReviewGroup;
import ru.yandex.market.modelparams.reviews.ReviewUtil;
import ru.yandex.market.modelparams.reviews.ReviewsSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author traff
 */
public class ReviewsTest extends TestCase {
    public void testReviews() {
        List<ModelReview> reviews = new ArrayList<>();
        reviews.add(new ModelReview(1, "a2", "www.a.ru/2"));
        reviews.add(new ModelReview(1, "a1", "www.a.ru/1"));
        reviews.add(new ModelReview(3, "b2", "www.b.ru/2"));
        reviews.add(new ModelReview(4, "b2-2", "www.b.ru/2"));
        reviews.add(new ModelReview(5, "b1", "www.b.ru/1"));
        reviews.add(new ModelReview(6, "b1-2", "www.b.ru/1"));

        List<ReviewGroup> group = new ArrayList<ReviewGroup>(ReviewsSerializer.groupByResource(reviews));

        assertEquals(2, group.size());
        assertEquals("www.a.ru", group.get(0).getResource());
        assertEquals("www.b.ru", group.get(1).getResource());
        assertEquals(2, group.get(0).getReviews().size());
        assertEquals(2, group.get(1).getReviews().size());

        assertEquals("a2", group.get(0).getReviews().get(0).getName());
        assertEquals("a1", group.get(0).getReviews().get(1).getName());

        assertEquals("b2", group.get(1).getReviews().get(0).getName());
        assertEquals("b1", group.get(1).getReviews().get(1).getName());
    }

    /**
     * Проверка, что порядок {@link ModelReview} сохраняется после выделения {@link ReviewGroup}
     */
    public void testOrder() {
    	List<ModelReview> reviews = new ArrayList<ModelReview>();
    	int modelId = 42;
        reviews.add(new ModelReview(modelId, "c1", "www.c.ru/2"));
        reviews.add(new ModelReview(modelId, "a1", "www.a.ru/1"));
        reviews.add(new ModelReview(modelId, "b1", "www.b.ru/1"));
        reviews.add(new ModelReview(modelId, "b2", "www.b.ru/2"));
        reviews.add(new ModelReview(modelId, "a2", "www.a.ru/2"));
        reviews.add(new ModelReview(modelId, "b3", "www.b.ru/3"));
        
        List<ReviewGroup> group= new ArrayList<ReviewGroup>(ReviewsSerializer.groupByResource(reviews));    
                
        assertEquals("c1", group.get(0).getReviews().get(0).getName());
        assertEquals("a1", group.get(1).getReviews().get(0).getName());
        assertEquals("a2", group.get(1).getReviews().get(1).getName());
        assertEquals("b1", group.get(2).getReviews().get(0).getName());
        assertEquals("b2", group.get(2).getReviews().get(1).getName());
        assertEquals("b3", group.get(2).getReviews().get(2).getName());
    }
    
    public void testNameRefinement() {
        String name = "[07.11.2007 5:13:49] Магнит желаний. Обзор GSM/UMTS-смартфона Nokia N95. Часть вторая";
        String refined = "Магнит желаний. Обзор GSM/UMTS-смартфона Nokia N95. Часть вторая";
        assertEquals(refined, ReviewUtil.formatName(name));
        name = "Nokia N95 «Сделано в Китае» [04/07/07]";
        refined = "Nokia N95 «Сделано в Китае»";
        assertEquals(refined, ReviewUtil.formatName(name));
    }
}
