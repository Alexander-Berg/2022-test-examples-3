package ru.yandex.market.pers.list.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.ResultDto;
import ru.yandex.market.pers.list.BasketClientParams;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service
public class CollectionsItemsMvcMock extends AbstractMvcMocks {
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
                        .format("/unifav/items/%s/%s", owner.getUserIdType().name(), owner.getId()))
                        .param(BasketClientParams.REGION_ID, regionId != null ? String.valueOf(regionId) : null)
                        .param(BasketClientParams.SOURCE, src)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(item)),
                resultMatcher);
    }

    public String addItemsMvc(BasketOwner owner,
                              BasketReferenceItem item,
                              Integer regionId,
                              ResultMatcher resultMatcher) {
        return invokeAndGet(
                post(String
                        .format("/unifav/items/%s/%s", owner.getUserIdType().name(), owner.getId()))
                        .param(BasketClientParams.REGION_ID, regionId != null ? String.valueOf(regionId) : null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(item)),
                resultMatcher);
    }

    public String deleteItemsMvc(BasketOwner owner, ReferenceType referenceType, String referenceId) {
        return invokeAndGet(
            delete(String.format("/unifav/items/%s/%s", owner.getUserIdType().name(), owner.getIdLong()))
                .param(BasketClientParams.REFERENCE_TYPE, referenceType.name())
                .param(BasketClientParams.REFERENCE_ID, referenceId)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
            );
    }
}
