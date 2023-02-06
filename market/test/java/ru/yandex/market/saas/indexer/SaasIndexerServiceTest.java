package ru.yandex.market.saas.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import ru.yandex.common.util.IOUtils;
import ru.yandex.market.saas.indexer.document.SaasDocument;
import ru.yandex.market.saas.indexer.document.SaasDocumentProperty;
import ru.yandex.market.saas.indexer.document.SaasOptionType;
import ru.yandex.market.saas.indexer.document.SaasPropertyType;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SaasIndexerServiceTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSerializeNullDocument() throws IOException, JSONException {
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(null, SaasIndexerAction.REOPEN);
        JSONAssert.assertEquals(
                getReourceAsString("data/saas_request_body_without_documents.json"),
                mapper.writeValueAsString(body),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeSimpleDocument() throws IOException, JSONException {
        SaasDocument document = new SaasDocument("doc12345");
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.DELETE);
        JSONAssert.assertEquals(
                getReourceAsString("data/saas_request_body_simple_document.json"),
                mapper.writeValueAsString(body),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeDocument() throws IOException, JSONException {
        SaasDocument document = createDocument("some_doc");
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.MODIFY);
        JSONAssert.assertEquals(
                getReourceAsString("data/saas_request_body_some_document.json"),
                mapper.writeValueAsString(body),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeDocumentWithNullValues() throws IOException, JSONException {
        SaasDocument document = createDocument("contains_nulls");
        document.setProperty("null_property", new SaasDocumentProperty(null, SaasPropertyType.PROPERTY));
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.MODIFY);
        JSONAssert.assertEquals(
                getReourceAsString("data/saas_request_body_some_document_with_null_values.json"),
                mapper.writeValueAsString(body),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeDocumentWithArrays() throws IOException, JSONException {
        SaasDocument document = createDocument("contains_arrays");
        Set<Integer> someSet = new HashSet<>();
        someSet.add(0);
        someSet.add(15);
        someSet.add(215);
        document.setProperty("array_property", SaasDocumentProperty.buildPropertiesList(someSet,
                SaasPropertyType.PROPERTY,
                SaasPropertyType.SEARCH_ATTR_INT));
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.MODIFY);
        JSONAssert.assertEquals(
                getReourceAsString("data/saas_request_body_some_document_with_arrays.json"),
                mapper.writeValueAsString(body),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeDocumentWithOptions() throws IOException, JSONException {
        SaasDocument document = createDocument("contains_options");
        document.setOption(SaasOptionType.MIME_TYPE, "text/html");
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.MODIFY);
        JSONAssert.assertEquals(
            getReourceAsString("data/saas_request_body_some_document_with_options.json"),
            mapper.writeValueAsString(body),
            JSONCompareMode.NON_EXTENSIBLE);

    }

    private String getReourceAsString(String filename) throws IOException {
        return IOUtils.readInputStream(getClass().getClassLoader().getResourceAsStream(filename));
    }

    private SaasDocument createDocument(String id) {
        SaasDocument document = new SaasDocument(id);
        document.setProperty("text_property", new SaasDocumentProperty("value",
                SaasPropertyType.PROPERTY));
        document.setProperty("number_property", new SaasDocumentProperty(1234.34,
                SaasPropertyType.PROPERTY));
        document.setProperty("i_number_search_literal", new SaasDocumentProperty(12312,
                SaasPropertyType.PROPERTY,
                SaasPropertyType.SEARCH_ATTR_INT));
        document.setProperty("s_string_search_literal", new SaasDocumentProperty("some text",
                SaasPropertyType.PROPERTY,
                SaasPropertyType.SEARCH_ATTR_LITERAL));
        document.setProperty("s_hidden_search_literal", new SaasDocumentProperty("not a property",
                SaasPropertyType.SEARCH_ATTR_LITERAL));
        document.setProperty("number_grouping_attribute", new SaasDocumentProperty(4372,
                SaasPropertyType.PROPERTY,
                SaasPropertyType.GROUP_ATTR_INT));
        document.setProperty("string_grouping_attribute", new SaasDocumentProperty("another text",
                SaasPropertyType.PROPERTY,
                SaasPropertyType.GROUP_ATTR_LITERAL));
        document.setProperty("factor", new SaasDocumentProperty(0.576,
                SaasPropertyType.PROPERTY,
                SaasPropertyType.FACTOR));
        document.setProperty("i_complex_type", new SaasDocumentProperty(42,
                SaasPropertyType.PROPERTY,
                SaasPropertyType.SEARCH_ATTR_INT,
                SaasPropertyType.GROUP_ATTR_INT));
        return document;
    }

}
