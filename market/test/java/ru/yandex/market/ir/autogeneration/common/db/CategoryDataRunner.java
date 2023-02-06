package ru.yandex.market.ir.autogeneration.common.db;

import java.util.Set;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.MboParameters;


/**
 * Runner для проверки тэгов на параметрах
 */
public class CategoryDataRunner {

    @Ignore
    @Test
    public void loadTags() throws InvalidProtocolBufferException {
        var categoryParametersService = new CategoryParametersServiceStub();
        categoryParametersService.setHost("http://mbo-http-exporter.yandex.net:8084/cached/categoryParameters/");
        categoryParametersService.setConnectionTimeoutMillis(300000);
        categoryParametersService.setTriesBeforeFail(1);

        var response = categoryParametersService.getParametersBytes(
                MboParameters.GetCategoryParametersRequest.newBuilder()
                        .setCategoryId(7811897)
                        .build()
        );

        var category = MboParameters.Category.parseFrom(response.getValue());

        var categoryData = CategoryData.build(category, Set.of("состав"));
        System.out.println(categoryData.getParamById(7978745L));
        System.out.println(categoryData.getParamById(29830890L));
    }
}
