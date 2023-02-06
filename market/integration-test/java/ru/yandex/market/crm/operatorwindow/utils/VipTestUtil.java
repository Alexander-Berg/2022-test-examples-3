package ru.yandex.market.crm.operatorwindow.utils;

import java.util.Map;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.domain.Email;
import ru.yandex.market.crm.operatorwindow.jmf.entity.VipCustomerAttribute;
import ru.yandex.market.jmf.bcp.BcpService;

@Component
public class VipTestUtil {

    private final BcpService bcpService;

    public VipTestUtil(BcpService bcpService) {
        this.bcpService = bcpService;
    }

    public void registerPhoneAsVip(Phone phone) {
        Map<String, Object> attributes = ru.yandex.market.jmf.utils.Maps.of(
                VipCustomerAttribute.ATTRIBUTE_TYPE, VipCustomerAttribute.ATTRIBUTE_TYPE_PHONE,
                VipCustomerAttribute.ATTRIBUTE_VALUE, phone.getRawOrNormalized()
        );

        bcpService.create(VipCustomerAttribute.FQN, attributes);
    }

    public void registerEmailAsVip(Email vipEmail) {
        Map<String, Object> attributes = ru.yandex.market.jmf.utils.Maps.of(

                VipCustomerAttribute.ATTRIBUTE_TYPE, VipCustomerAttribute.ATTRIBUTE_TYPE_EMAIL,
                VipCustomerAttribute.ATTRIBUTE_VALUE, vipEmail.getAddress()
        );

        bcpService.create(VipCustomerAttribute.FQN, attributes);
    }
}
