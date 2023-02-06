package ru.yandex.market.pers.author.yql;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.author.takeout.model.TakeoutParam;
import ru.yandex.market.pers.yt.yqlgen.YqlLoader;
import ru.yandex.yt.yqltest.YqlTestScript;
import ru.yandex.yt.yqltest.spring.AbstractYqlTest;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 02.11.2021
 */
public class TakeoutYqlTest extends AbstractYqlTest {

    @Test
    public void testTakeoutStatus() {
        runTest(
            YqlTestScript.simple(YqlLoader.readYqlWithLib("/yql/takeout/lib_takeout.sql"))
                .requestProperty("result"),
            "/takeout/takeout_status_expected.json",
            "/takeout/takeout_status.mock"
        );
    }

    @Test
    public void testUserTakeout() {
        runTest(
            YqlTestScript.simple(YqlLoader.readYqlWithLib("/yql/takeout/lib_takeout.sql")
                .replace(TakeoutParam.USER_ID.getYqlFilter(), "1"))
                .requestProperty("uid_result"),
            "/takeout/takeout_struct_uid_expected.json",
            "/takeout/takeout_struct.mock"
        );
    }

    @Test
    public void testModelTakeout() {
        runTest(
            YqlTestScript.simple(YqlLoader.readYqlWithLib("/yql/takeout/lib_takeout.sql")
                .replace(TakeoutParam.MODEL_ID.getYqlFilter(), "1"))
                .requestProperty("model_result"),
            "/takeout/takeout_struct_model_expected.json",
            "/takeout/takeout_struct.mock"
        );
    }

    @Test
    public void testShopWithBusinessTakeout() {
        runTest(
            YqlTestScript.simple(YqlLoader.readYqlWithLib("/yql/takeout/lib_takeout.sql")
                .replace(TakeoutParam.SHOP_ID.getYqlFilter(), "1"))
                .requestProperty("shop_result"),
            "/takeout/takeout_struct_shop_expected.json",
            "/takeout/takeout_struct.mock"
        );
    }

    @Test
    public void testShopNoBusinessTakeout() {
        runTest(
            YqlTestScript.simple(YqlLoader.readYqlWithLib("/yql/takeout/lib_takeout.sql")
                .replace(TakeoutParam.SHOP_ID.getYqlFilter(), "4"))
                .requestProperty("shop_result"),
            "/takeout/takeout_struct_shop_nob_expected.json",
            "/takeout/takeout_struct.mock"
        );
    }

    @Test
    public void testBusinessTakeout() {
        runTest(
            YqlTestScript.simple(YqlLoader.readYqlWithLib("/yql/takeout/lib_takeout.sql")
                .replace(TakeoutParam.BUSINESS_ID.getYqlFilter(), "2"))
                .requestProperty("business_result"),
            "/takeout/takeout_struct_business_expected.json",
            "/takeout/takeout_struct.mock"
        );
    }
}
