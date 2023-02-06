package ru.yandex.direct.core.entity.testuser.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import ru.yandex.direct.rbac.RbacRole;

public class TestUserRoleSerializer extends JsonSerializer<RbacRole> {

    @Override
    public void serialize(RbacRole value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(RbacRole.toSource(value).getLiteral());
    }

}
