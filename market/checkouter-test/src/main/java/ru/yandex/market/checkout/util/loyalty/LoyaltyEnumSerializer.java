package ru.yandex.market.checkout.util.loyalty;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import ru.yandex.market.loyalty.api.model.EnumWithPermanentCode;

public class LoyaltyEnumSerializer implements JsonSerializer<EnumWithPermanentCode> {


    @Override
    public JsonElement serialize(EnumWithPermanentCode src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getCode());
    }
}
