package ru.yandex.market.pers.list.mock;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.basket.model.BasketItemsDtoList;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.ItemReference;
import ru.yandex.market.pers.basket.model.ResultDto;
import ru.yandex.market.pers.list.BasketClientParams;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 28.09.2020
 */
@Service
public class BasketWidgetItemsMvcMocks extends AbstractMvcMocks {
    public List<BasketReferenceItem> getExistingItems(BasketOwner owner,
                                                      MarketplaceColor color,
                                                      List<ItemReference> items) {
        BasketItemsDtoList response = parseValue(getExistingItemsMvc(owner, color, items, null),
            new TypeReference<>() {
            });
        return response.getItems();
    }

    public String getExistingItemsMvc(BasketOwner owner, MarketplaceColor color, List<ItemReference> items, Boolean noCache) {
        return invokeAndGet(
            post(String
                .format("/widget/items/%s/%s/existing", owner.getUserIdType().name(), owner.getId()))
                .param(BasketClientParams.PARAM_RGB, color.getName())
                .param("noCache", noCache == null ? null : noCache.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FormatUtils.toJson(items)),
            status().is2xxSuccessful());
    }

    public BasketReferenceItem addItem(BasketOwner owner, BasketReferenceItem item) {
        return addItem(owner, item, null, null);
    }

    public BasketReferenceItem addItem(BasketOwner owner, BasketReferenceItem item, Integer regionId) {
        return addItem(owner, item, regionId, null);
    }

    public BasketReferenceItem addItem(BasketOwner owner, BasketReferenceItem item, Integer regionId, String src) {
        ResultDto<BasketReferenceItem> response = parseValue(
                addItemsMvc(owner, item, regionId, src, status().is2xxSuccessful()),
                new TypeReference<>() {
                });
        return response.getResult();
    }

    public String addItemsMvc(BasketOwner owner,
                              BasketReferenceItem item,
                              Integer regionId,
                              String src,
                              ResultMatcher resultMatcher) {
        return invokeAndGet(
            post(String
                .format("/widget/items/%s/%s", owner.getUserIdType().name(), owner.getId()))
                .param(BasketClientParams.REGION_ID, regionId != null ? String.valueOf(regionId) : null)
                .param(BasketClientParams.SOURCE, src)
                .contentType(MediaType.APPLICATION_JSON)
                .content(FormatUtils.toJson(item)),
            resultMatcher);
    }

    public String deleteItemsMvc(BasketOwner owner, long itemId) {
        return deleteItemsMvc(owner, itemId, null);
    }

    public String deleteItemsMvc(BasketOwner owner, long itemId, Integer regionId) {
        return invokeAndGet(
            delete(String.format("/widget/items/%s/%s/%s", owner.getUserIdType().name(), owner.getId(), itemId))
                .param(BasketClientParams.REGION_ID, regionId != null ? String.valueOf(regionId) : null)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }
}
