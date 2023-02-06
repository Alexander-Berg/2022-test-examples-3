package ru.yandex.market.psku.postprocessor;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryServiceStub;

/**
 * Runner для проверки ответа от MboCategoryServiceStub
 */
@Ignore
public class MboCategoryServiceRunner {


    @Test
    public void run() {
        MboCategoryServiceStub service = new MboCategoryServiceStub();
        service.setHost("http://cm-api.vs.market.yandex.net/proto/mboCategoryService/");

        var result = service.getCategoryGroups(
                MboCategory.GetCategoryGroupsRequest.newBuilder().build()
        );

        System.out.println(result);

    }
}
