package ru.yandex.direct.core.testing.steps;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.core.testing.data.TestContentPromotionCommonData.defaultContentPromotion;

@ParametersAreNonnullByDefault
public class ContentPromotionSteps {
    private final ContentPromotionRepository contentPromotionRepository;

    @Autowired
    public ContentPromotionSteps(ContentPromotionRepository contentPromotionRepository) {
        this.contentPromotionRepository = contentPromotionRepository;
    }

    public ContentPromotionContent createContentPromotionContent(ClientId clientId, ContentPromotionContent content) {
        long contentId = contentPromotionRepository.insertContentPromotion(clientId, content);
        return contentPromotionRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
    }

    public ContentPromotionContent createContentPromotionContent(ClientId clientId, ContentPromotionContentType type) {
        ContentPromotionContent content = defaultContentPromotion(clientId, type);
        return createContentPromotionContent(clientId, content);
    }
}
