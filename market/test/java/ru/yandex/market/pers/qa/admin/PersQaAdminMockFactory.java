package ru.yandex.market.pers.qa.admin;

import java.util.Optional;

import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.shopinfo.ShopInfoService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class PersQaAdminMockFactory {

    public static BlackBoxService blackBoxMock() {
        return PersTestMocksHolder.registerMock(BlackBoxService.class, source-> {
            try {
                BlackBoxUserInfo value = new BlackBoxUserInfo(1);
                value.addField(UserInfoField.LOGIN, null);
                when(source.getUserInfo(anyLong(), any())).thenReturn(value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }


    public static ShopInfoService shopInfoServiceMock() {
        return PersTestMocksHolder.registerMock(ShopInfoService.class, source-> {
            try {
                when(source.getShopInfo(anyLong())).thenReturn(Optional.empty());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static CatalogerClient catalogerClientMock() {
        return PersTestMocksHolder.registerMock(CatalogerClient.class, source-> {
            try {
                when(source.getBrandName(anyLong())).thenReturn(Optional.empty());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }


}
