package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.ExtensionStatusSelectionEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.ExtensionStatus;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetRequestConverter.convertExtensionStatuses;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetRequestConverterConvertExtensionStatusesTest {

    @Parameterized.Parameter
    public List<ExtensionStatusSelectionEnum> extensionStatuses;

    @Parameterized.Parameter(1)
    public List<ExtensionStatus> expectedExtensionStatuses;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {emptyList(), emptyList()},
                {singletonList(ExtensionStatusSelectionEnum.ACCEPTED), singletonList(ExtensionStatus.ACCEPTED)},
                {singletonList(ExtensionStatusSelectionEnum.DRAFT), singletonList(ExtensionStatus.DRAFT)},
                {singletonList(ExtensionStatusSelectionEnum.MODERATION), singletonList(ExtensionStatus.MODERATION)},
                {singletonList(ExtensionStatusSelectionEnum.REJECTED), singletonList(ExtensionStatus.REJECTED)},
                {asList(ExtensionStatusSelectionEnum.values()),
                        asList(ExtensionStatus.ACCEPTED, ExtensionStatus.DRAFT, ExtensionStatus.MODERATION,
                                ExtensionStatus.REJECTED)},
                // direct enumeration 'cause not all elements of ExtensionStatus enum can be returned from convertation
        };
    }

    @Test
    public void test() {
        assertThat(convertExtensionStatuses(extensionStatuses))
                .containsExactlyInAnyOrder(expectedExtensionStatuses.toArray(new ExtensionStatus[0]));
    }
}
