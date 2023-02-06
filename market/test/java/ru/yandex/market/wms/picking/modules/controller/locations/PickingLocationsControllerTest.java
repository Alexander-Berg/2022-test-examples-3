package ru.yandex.market.wms.picking.modules.controller.locations;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;

import ru.yandex.market.wms.common.spring.IntegrationTest;
// Какой хороший мир (с) Дима

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/2/zones.xml", connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/fixtures/autostart/2/pick_locations.xml", connection = "wmwhseConnection"),
})
class PickingLocationsControllerTest extends IntegrationTest {

    public static final boolean STRICT = true;

//    @Test
//    public void get__exists() throws Exception {
//        //language=JSON5
////        String expected = "{"
////                + "loc:'C4-10-0001',"
////                + "locationType:'PICK',"
////                + "logicalLocation:'100001',"
////                + "loseId:false,"
////                + "putawayzone:'FLOOR',"
////                + "transporterLoc:null,"
////                + "conveyorLoc:null,"
////                + "addwho:'test',"
////                + "editwho:'test'"
////                + "}";
//        mockMvc.perform(get("/check-location/C4-10-0001"))
//                .andExpect(status().isOk())
////              .andExpect(content().json(expected, STRICT))
//        ;
//    }
//
//    @Test
//    public void get__missing() throws Exception {
//        //language=JSON5
//        String expected = "{message:'404 NOT_FOUND \"Incorrect location: C4-10-9999\"',status:'NOT_FOUND',
//        resourceType:'LOC',wmsErrorCode:'LOCATION_NOT_FOUND'}";
//        mockMvc.perform(get("/check-location/C4-10-9999"))
//                .andExpect(status().isNotFound())
//                .andExpect(content().json(expected, STRICT))
//        ;
//    }
}
