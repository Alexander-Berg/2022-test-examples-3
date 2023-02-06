package ru.yandex.direct.logicprocessor.processors.bsexport.resources;

import java.util.List;

import one.util.streamex.EntryStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler.BannerHrefHandler;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler.BannerTitleAndBodyHandler;
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = EssLogicProcessorTestConfiguration.class)
@ExtendWith(SpringExtension.class)
class BsExportBannerResourcesServiceTest {

    @Autowired
    private BsExportBannerResourcesService service;

    @Test
    void getHandlerToObjectMapTest() {
        var object1 = new BsExportBannerResourcesObject.Builder()
                .setBid(2L)
                .setResourceType(BannerResourceType.BANNER_TITLE_AND_BODY)
                .build();
        var object2 = new BsExportBannerResourcesObject.Builder()
                .setBid(3L)
                .setResourceType(BannerResourceType.BANNER_TITLE_AND_BODY)
                .build();
        var object3 = new BsExportBannerResourcesObject.Builder()
                .setBid(3L)
                .setResourceType(BannerResourceType.BANNER_HREF)
                .build();

        var objects = List.of(object1, object2, object3);
        var handlerToObjectsMap = service.getHandlerToObjectsMap(objects);

        var handlerClassToObjectsMap = EntryStream.of(handlerToObjectsMap)
                .mapKeys(handler -> (Class) handler.getClass())
                .toMap();

        assertThat(handlerClassToObjectsMap)
                .containsOnlyKeys(BannerTitleAndBodyHandler.class, BannerHrefHandler.class);

        assertThat(handlerClassToObjectsMap.get(BannerTitleAndBodyHandler.class))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(object1, object2);

        assertThat(handlerClassToObjectsMap.get(BannerHrefHandler.class))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(object3);
    }

    @Test
    void objectWithUnknownEnumTypeTest() {
        var object = new BsExportBannerResourcesObject.Builder()
                .setBid(2L)
                .setResourceType(BannerResourceType.UNKNOWN)
                .build();

        var objects = List.of(object);
        var handlerToObjectsMap = service.getHandlerToObjectsMap(objects);
        assertThat(handlerToObjectsMap).isEmpty();
    }
}
