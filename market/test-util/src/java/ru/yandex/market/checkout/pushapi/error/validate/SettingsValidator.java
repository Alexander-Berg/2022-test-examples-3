package ru.yandex.market.checkout.pushapi.error.validate;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.notNull;

@Component
public class SettingsValidator implements Validator<Settings> {
    @Override
    public void validate(Settings object) throws ValidationException {
        notNull(object, "settings is null");
        notNull(object.isPartnerInterface(), "isPartnerInterface is null");

        if(!object.isPartnerInterface()) {
            notNull(object.getAuthToken(), "authToken is null");
            notNull(object.getAuthType(), "authType is null");
            notNull(object.getDataType(), "dataType is null");
            notNull(object.getUrlPrefix(), "urlPrefix is null");
        }
    }
}
