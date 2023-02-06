package ru.yandex.market.partner.auction.matchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.xml.impl.NamedContainer;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * feature-матчеры для {@link MockServResponse}.
 *
 * @author vbudnev
 */
public class MockServResponseFeatureMatchers {

    @Factory
    public static Matcher<MockServResponse> hasErrors() {
        return new FeatureMatcher<MockServResponse, Boolean>(
                equalTo(true),
                "response has errors",
                "response has no errors"
        ) {
            @Override
            protected Boolean featureValueOf(final MockServResponse actual) {
                return actual.hasErrors();
            }
        };
    }

    @Factory
    public static Matcher<MockServResponse> hasWarnings() {
        return new FeatureMatcher<MockServResponse, Boolean>(
                equalTo(true),
                "warning block is not empty",
                "warning block is empty"
        ) {
            @Override
            protected Boolean featureValueOf(final MockServResponse actual) {
                for (Object data : actual.getData()) {
                    NamedContainer warningsBlock = findNamedContainerBlock(data, "warnings");
                    if (warningsBlock != null) {
                        List<NamedContainer> warnings = tryExtractContainersList(warningsBlock);
                        return CollectionUtils.isNotEmpty(warnings);
                    }
                }
                return false;
            }
        };
    }

    /**
     * Ответ содержит блок warning с определенным содержимым.
     *
     * @param warningType - имя блока
     * @param warningItem - содержимое блока
     */
    @Factory
    public static Matcher<MockServResponse> hasWarningOfTypeWithItem(String warningType, String warningItem) {
        return new FeatureMatcher<MockServResponse, Boolean>(
                equalTo(true),
                "warning of type " + warningType + " for item " + warningItem,
                "no warning of type " + warningType + " for item " + warningItem
        ) {
            @Override
            protected Boolean featureValueOf(final MockServResponse actual) {
                for (Object data : actual.getData()) {
                    NamedContainer warningsBlock = findNamedContainerBlock(data, "warnings");
                    if (warningsBlock != null) {
                        List<NamedContainer> warnings = tryExtractContainersList(warningsBlock);
                        List<String> itemsList = getWarnings(warnings).get(warningType);
                        return itemsList != null && itemsList.contains(warningItem);
                    }
                }
                return false;
            }
        };
    }

    @Factory
    public static Matcher<MockServResponse> hasTotalCount(Integer expectedFieldValue) {
        return hasNumericFieldWithValue("total-count", expectedFieldValue);
    }

    @Factory
    public static Matcher<MockServResponse> hasValidBidCount(Integer expectedFieldValue) {
        return hasNumericFieldWithValue("valid-bid-count", expectedFieldValue);
    }

    @Factory
    public static Matcher<MockServResponse> hasTypeChangedCount(Integer expectedFieldValue) {
        return hasNumericFieldWithValue("type-changed-count", expectedFieldValue);
    }

    @Factory
    public static Matcher<MockServResponse> hasBidResetCount(Integer expectedFieldValue) {
        return hasNumericFieldWithValue("bid-reset-count", expectedFieldValue);
    }

    @Factory
    public static Matcher<MockServResponse> hasFoundBidCount(Integer expectedFieldValue) {
        return hasNumericFieldWithValue("found-bid-count", expectedFieldValue);
    }

    @Factory
    public static Matcher<MockServResponse> hasBidUpdateCount(Integer expectedFieldValue) {
        return hasNumericFieldWithValue("bid-update-count", expectedFieldValue);
    }

    @Factory
    public static Matcher<MockServResponse> hasFinalBidUpdateCount(Integer expectedFieldValue) {
        return hasNumericFieldWithValue("final-bid-update-count", expectedFieldValue);
    }

    @Factory
    public static Matcher<MockServResponse> hasNumericFieldWithValue(String expectedFieldName, Integer expectedFieldValue) {
        return new FeatureMatcher<MockServResponse, Integer>(
                equalTo(expectedFieldValue),
                "field with name " + expectedFieldName,
                "but field with name " + expectedFieldName
        ) {
            @Override
            protected Integer featureValueOf(final MockServResponse actual) {
                for (Object data : actual.getData()) {
                    NamedContainer fieldBlock = findNamedContainerBlock(data, expectedFieldName);
                    if (fieldBlock != null && fieldBlock.getContent() instanceof Integer) {
                        return (Integer) fieldBlock.getContent();
                    }
                }
                return null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static List<NamedContainer> tryExtractContainersList(@Nonnull NamedContainer ctr) {
        if (ctr.getContent() instanceof List) {
            List<NamedContainer> warnings = (List<NamedContainer>) ctr.getContent();
            if (CollectionUtils.isNotEmpty(warnings)) {
                return warnings;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getWarnings(List<NamedContainer> warningContainers) {
        Map<String, List<String>> warnings = new HashMap<>();

        for (NamedContainer nc : warningContainers) {
            if (nc.getContent() instanceof List) {
                warnings.put(nc.name(), (List<String>) nc.getContent());
            }
        }
        return warnings;
    }

    private static NamedContainer findNamedContainerBlock(@Nullable Object root, @Nonnull String namedCtrBlockName) {
        if (root instanceof NamedContainer) {
            NamedContainer ctr = (NamedContainer) root;
            String ctrName = ctr.name();
            if (namedCtrBlockName.equals(ctrName)) {
                return ctr;
            }
        }
        return null;
    }

}
