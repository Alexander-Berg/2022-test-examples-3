package ru.yandex.market.gutgin.tms.assertions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.ProtocolMessage;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.utils.MessageUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author danfertev
 * @since 29.07.2019
 */
public class DataBucketAssertions extends AbstractObjectAssert<DataBucketAssertions, PartnerContent.BucketProcessInfo> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public DataBucketAssertions(PartnerContent.BucketProcessInfo bucketProcessInfo) {
        super(bucketProcessInfo, DataBucketAssertions.class);
    }

    public DataBucketAssertions hasStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus expectedStatus) {
        super.isNotNull();
        Assertions.assertThat(actual.getResultReportStatus()).isEqualTo(expectedStatus);
        return myself;
    }

    public DataBucketAssertions hasNoValidationErrors() {
        super.isNotNull();
        Assertions.assertThat(actual.getValidationErrorList()).isEmpty();
        return myself;
    }

    public DataBucketAssertions containsValidationErrors(MessageInfo... expectedMessageInfos) {
        super.isNotNull();
        Assertions.assertThat(actual.getValidationErrorList())
            .containsExactlyInAnyOrderElementsOf(getProtoMessages(expectedMessageInfos));
        return myself;
    }

    public DataBucketAssertions hasNoModelCreatedMessages() {
        super.isNotNull();
        Assertions.assertThat(actual.getModelCreatedInfoList()).isEmpty();
        return myself;
    }

    public DataBucketAssertions hasCreatedModelMessagesSize(int expectedSize) {
        super.isNotNull();
        Assertions.assertThat(actual.getModelCreatedInfoCount()).isEqualTo(expectedSize);
        return myself;
    }

    public ListAssert<CreatedModel> getCreatedModelIds() {
        super.isNotNull();
        List<CreatedModel> createdModels = actual.getModelCreatedInfoList().stream()
            .map(m -> {
                Map<String, Object> params = readParams(m.getParams());
                String shopSku = (String) params.get("shopSku");
                long modelId = ((Integer) params.get("resultMboPskuId")).longValue();
                return new CreatedModel(shopSku, modelId);
            })
            .collect(Collectors.toList());
        return Assertions.assertThat(createdModels);
    }

    private static List<ProtocolMessage.Message> getProtoMessages(MessageInfo... expectedMessageInfos) {
        return Arrays.stream(expectedMessageInfos).map(MessageUtils::convertToProto).collect(Collectors.toList());
    }

    private static Map<String, Object> readParams(String params) {
        try {
            return OBJECT_MAPPER.readValue(params, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static class CreatedModel {
        private final String shopSku;
        private final long modelId;

        CreatedModel(String shopSku, long modelId) {
            this.shopSku = shopSku;
            this.modelId = modelId;
        }

        public String getShopSku() {
            return shopSku;
        }

        public long getModelId() {
            return modelId;
        }

        @Override
        public String toString() {
            return "CreatedModel{" +
                "shopSku='" + shopSku + '\'' +
                ", modelId=" + modelId +
                '}';
        }
    }
}
