package ru.yandex.market.mbo.flume.sink.saas;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import com.googlecode.protobuf.format.JsonFormat;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.flume.sink.converter.JsonModel;
import ru.yandex.market.mbo.flume.sink.converter.ModelSaasDocumentConverter;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.search.saas.RTYServer;
import ru.yandex.search.saas.SearchZone;


public class ConversionTests {

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void modelToTMessageTest() throws IOException {
        InputStreamReader amodelReader = new InputStreamReader(
                Objects.requireNonNull(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("amodel.json"))
        );
        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder();
        JsonFormat.merge(amodelReader, modelBuilder);
        ModelSaasDocumentConverter documentConverter = new ModelSaasDocumentConverter();
        JsonModel jsonModel = new JsonModel(modelBuilder);
        RTYServer.TMessage.TDocument.Builder builder = documentConverter.convert(jsonModel);
        RTYServer.TMessage.TDocument doc = builder.build();
        System.out.println("doc = " + doc);

        Assert.assertEquals(doc.getUrl(), ModelSaasDocumentConverter.getModelUrl(modelBuilder));
        Assert.assertNotEquals(0, doc.getSearchAttributesCount());
        List<SearchZone.TAttribute> searchAttributesList = doc.getSearchAttributesList();
        for (SearchZone.TAttribute a : searchAttributesList) {
            // LITERAL_ATTRIBUTE length must be <= 256 bytes, assuming utf-8 two byte chars
            Assert.assertFalse(a.getValue().length() > 127);
        }

    }

    @Test
    public void modelToTMessageFailTest() throws IOException {
        InputStreamReader amodelReader = new InputStreamReader(
                Objects.requireNonNull(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("abadmodel.json"))
        );
        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder();
        JsonFormat.merge(amodelReader, modelBuilder);
        ModelSaasDocumentConverter documentConverter = new ModelSaasDocumentConverter();
        JsonModel jsonModel = new JsonModel(modelBuilder);
        RTYServer.TMessage.TDocument.Builder builder = documentConverter.convert(jsonModel);
        Assert.assertNull(builder);

    }


}
