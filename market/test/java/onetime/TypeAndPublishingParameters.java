package onetime;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.yandex.market.markup2.entries.group.ModelTypeValue;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.group.PublishingValue;

import java.util.HashMap;
import java.util.Map;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 06.09.2017
 */
public class TypeAndPublishingParameters {
    public enum Type {
        GURU(ModelTypeValue.GURU),
        CLUSTER(ModelTypeValue.CLUSTERS),
        ALL(ModelTypeValue.ALL);

        private final ModelTypeValue parameterValue;

        Type(ModelTypeValue parameterValue) {
            this.parameterValue = parameterValue;
        }

        public ModelTypeValue getParameterValue() {
            return parameterValue;
        }
    }

    public enum Visibility {
        ALL(PublishingValue.ALL),
        PUBLISHED(PublishingValue.PUBLISHED),
        UNPUBLISHED(PublishingValue.UNPUBLISHED);

        private final PublishingValue parameterValue;

        Visibility(PublishingValue parameterValue) {
            this.parameterValue = parameterValue;
        }

        public PublishingValue getParameterValue() {
            return parameterValue;
        }
    }

    private Type type = Type.ALL;
    private Visibility visibility = Visibility.ALL;

    @JsonProperty("type")
    public Type getType() {
        return type;
    }

    @JsonProperty("visibility")
    public Visibility getVisibility() {
        return visibility;
    }

    public static Map<ParameterType, Object> toParameterTypeMap(TypeAndPublishingParameters configParameters) {
        Map<ParameterType, Object> parametersMap = new HashMap<>();
        ModelTypeValue modelTypeValue = ModelTypeValue.ALL;
        PublishingValue publishingValue = PublishingValue.ALL;
        if (configParameters != null) {
            modelTypeValue = configParameters.getType().getParameterValue();
            publishingValue = configParameters.getVisibility().getParameterValue();
        }

        parametersMap.put(ParameterType.MODEL_TYPE, modelTypeValue);
        parametersMap.put(ParameterType.PUBLISHING, publishingValue);

        return parametersMap;
    }

    @Override
    public String toString() {
        return "TypeAndPublishingParameters{" +
            "type=" + type +
            ", visibility=" + visibility +
            '}';
    }
}
