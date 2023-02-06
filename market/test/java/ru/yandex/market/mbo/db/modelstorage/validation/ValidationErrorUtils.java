package ru.yandex.market.mbo.db.modelstorage.validation;

import com.google.common.collect.Maps;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author danfertev
 */
public class ValidationErrorUtils {
    private ValidationErrorUtils() {
    }

    public static void assertValidationError(ModelValidationError error, ModelValidationError.ErrorType type,
                                             ModelValidationError.ErrorSubtype subtype,
                                             boolean critical, Map<ModelStorage.ErrorParamName, String> params) {
        assertValidationError(error, type, critical, params);
        assertEquals(subtype, error.getSubtype());
    }

    public static void assertValidationError(ModelValidationError error, ModelValidationError.ErrorType type,
                                       boolean critical, Map<ModelStorage.ErrorParamName, String> params) {
        assertEquals(type, error.getType());
        assertEquals(critical, error.isCritical());
        assertTrue((params != null && error.getParams() != null &&
            Maps.difference(params, error.getParams()).areEqual()) ||
            (params == null && error.getParams() == null));
    }

    public static void assertValidationError(ModelValidationError error, ModelValidationError.ErrorType type,
                                       ModelValidationError.ErrorSubtype subtype,
                                       boolean critical, String... params) {
        assertEquals(type, error.getType());
        assertEquals(subtype, error.getSubtype());
        assertEquals(critical, error.isCritical());
        assertEquals(params.length, error.getParams().size());
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], error.getParams().values().toArray(new String[0])[i]);
        }
    }

    public static void assertValidationError(ModelValidationError error, Long modelId,
                                             ModelValidationError.ErrorType type,
                                             ModelValidationError.ErrorSubtype subtype,
                                             boolean critical) {
        assertEquals(modelId, error.getModelId());
        assertEquals(type, error.getType());
        assertEquals(subtype, error.getSubtype());
        assertEquals(critical, error.isCritical());
    }
}
