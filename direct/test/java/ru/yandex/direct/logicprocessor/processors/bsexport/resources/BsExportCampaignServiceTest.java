package ru.yandex.direct.logicprocessor.processors.bsexport.resources;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.BsExportCampaignService;
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration;

@ContextConfiguration(classes = EssLogicProcessorTestConfiguration.class)
@ExtendWith(SpringExtension.class)
class BsExportCampaignServiceTest {

    @Autowired
    private BsExportCampaignService service;
//
//    @Test
//    void getHandlerToObjectMapTest() {
//        var object1 = new BsExportCampaignObject.Builder()
//                .setCid(2L)
//                .setCampaignResourceType(CampaignResourceType.COMMON_FIELDS)
//                .build();
//        var object2 = new BsExportCampaignObject.Builder()
//                .setCid(3L)
//                .setCampaignResourceType(CampaignResourceType.COMMON_FIELDS)
//                .build();
//        var object3 = new BsExportCampaignObject.Builder()
//                .setCid(3L)
//                .setCampaignResourceType(CampaignResourceType.CAMPAIGN_STRATEGY)
//                .build();
//
//        var objects = List.of(object1, object2, object3);
//        var handlerToObjectsMap = service.getCampaignHandlerToObjectsMap(objects);
//
//        var handlerClassToObjectsMap = EntryStream.of(handlerToObjectsMap)
//                .mapKeys(handler -> (Class) handler.getClass())
//                .toMap();
//
//        assertThat(handlerClassToObjectsMap)
//                .containsOnlyKeys(CampaignCommonFieldsHandler.class, CampaignStrategyHandler.class);
//
//        assertThat(handlerClassToObjectsMap.get(CampaignCommonFieldsHandler.class))
//                .usingRecursiveFieldByFieldElementComparator()
//                .containsExactlyInAnyOrder(object1, object2);
//
//        assertThat(handlerClassToObjectsMap.get(CampaignStrategyHandler.class))
//                .usingRecursiveFieldByFieldElementComparator()
//                .containsExactlyInAnyOrder(object3);
//    }

}
