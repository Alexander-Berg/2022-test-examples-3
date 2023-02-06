package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.MobileAppAdActionEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.MobileAppActionConverter.convertMobileAppAction;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class MobileAppActionConverterTest {

    @Parameterized.Parameter
    public NewMobileContentPrimaryAction internalAction;

    @Parameterized.Parameter(1)
    public MobileAppAdActionEnum externalAction;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {NewMobileContentPrimaryAction.BUY, MobileAppAdActionEnum.BUY_AUTODETECT},
                {NewMobileContentPrimaryAction.DOWNLOAD, MobileAppAdActionEnum.DOWNLOAD},
                {NewMobileContentPrimaryAction.GET, MobileAppAdActionEnum.GET},
                {NewMobileContentPrimaryAction.INSTALL, MobileAppAdActionEnum.INSTALL},
                {NewMobileContentPrimaryAction.MORE, MobileAppAdActionEnum.MORE},
                {NewMobileContentPrimaryAction.OPEN, MobileAppAdActionEnum.OPEN},
                {NewMobileContentPrimaryAction.PLAY, MobileAppAdActionEnum.PLAY},
                {NewMobileContentPrimaryAction.UPDATE, MobileAppAdActionEnum.UPDATE},
        };
    }

    @Test
    public void test() {
        assertThat(convertMobileAppAction(internalAction)).isEqualByComparingTo(externalAction);
    }

}
