package ru.yandex.market.ir.nirvana.ultracontroller.yt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.ir.nirvana.ultracontroller.yt.params.AppParams;
import ru.yandex.market.ir.nirvana.ultracontroller.yt.params.EnrichedOfferFields;
import ru.yandex.market.ir.nirvana.ultracontroller.yt.params.OfferFields;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Ignore
public class AppParamsTest {
    @Test
    public void saveSample() throws IOException, InvocationTargetException, IllegalAccessException {
        AppParams params = new AppParams();
        params.setSnapshotTransactionTimeoutHours(2);
        params.setSnapshotTransactionPingPeriodMinutes(1);
        params.setHeavyCommandsTimeoutHours(2);
        params.setOperationStatusPingTimeoutMinutes(1);

        params.setSourceYtApiHost("arnold.yt.yandex.net");
//        params.setSourceTablePath("//home/market/development/ir/USERS/vzay/watson/test_input");
        params.setSourceTablePath("//home/search-functionality/parsepl/market/flatten_results");

        params.setOutYtApiHost("hahn.yt.yandex.net");
        params.setOutTablesFolderPath("//home/market/users/vzay/watson_result");

        params.setUcHost("http://an.ultracontroller.vs.market.yandex.net:34563/");
        params.setUcUserAgent("vzay");

        //noinspection CheckStyle
        params.setBatchSize(1000);

        OfferFields offerFields = new OfferFields();
        for (Method m : OfferFields.class.getDeclaredMethods()) {
            if (m.getName().startsWith("set")) {
//                String fieldName = Character.toLowerCase(m.getName().charAt("set".length()))
//                    + m.getName().substring("set".length() + 1);
                m.invoke(offerFields, "");
            }
        }
        offerFields.setOffer("attributes_name");
        offerFields.setPrice("attributes_price");
        offerFields.setShopCategoryName("attributes_category");
        params.setOfferFields(offerFields);

        EnrichedOfferFields enrichedOfferFields = new EnrichedOfferFields();
        for (Method m : EnrichedOfferFields.class.getDeclaredMethods()) {
            if (m.getName().startsWith("set")) {
                m.invoke(enrichedOfferFields, "");
            }
        }
        enrichedOfferFields.setCategoryId("category_id");
        enrichedOfferFields.setModelId("model_id");
        enrichedOfferFields.setMarketSkuId("market_sku_id");
        params.setEnrichedOfferFields(enrichedOfferFields);

        params.setOfferFieldName(""); //"request");
        params.setEnrichedOfferFieldName(""); //"response");

        params.setTransferFields(new String[]{"host", "url"});

        params.setFilterByMarketSkuId(true);

        // Save file!
        File file = new File("src/test/resources/uc_config.json");
        System.out.println(file.getAbsolutePath());
        FileOutputStream out = new FileOutputStream(file);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(out, params);
    }
}
