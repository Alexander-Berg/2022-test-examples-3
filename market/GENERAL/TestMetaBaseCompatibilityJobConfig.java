package ru.yandex.market.tsum.pipelines.report.resources;

import java.util.UUID;

import org.springframework.data.annotation.PersistenceConstructor;

import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.inputs.TextField;

public class TestMetaBaseCompatibilityJobConfig implements Resource {
    @TextField(required = true)
    private String arcYavSecret;

    @TextField(required = true)
    private String arcYavSecretKey;

    private boolean testBaseSearch = false;

    @PersistenceConstructor
    public TestMetaBaseCompatibilityJobConfig() {
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("e72150aa-c9e5-4fec-81a5-6ec0a1a07d52");
    }

    public String getArcYavSecret() {
        return arcYavSecret;
    }

    private void setArcYavSecret(String secret) {
        arcYavSecret = secret;
    }

    public String getArcYavSecretKey() {
        return arcYavSecretKey;
    }

    private void setArcYavSecretKey(String secretKey) {
        arcYavSecretKey = secretKey;
    }

    public boolean getTestBaseSearch() {
        return testBaseSearch;
    }

    private void setTestBaseSearch(boolean value) {
        testBaseSearch = value;
    }
}
