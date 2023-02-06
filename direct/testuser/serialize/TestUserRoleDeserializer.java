package ru.yandex.direct.core.entity.testuser.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import one.util.streamex.StreamEx;

import ru.yandex.direct.rbac.RbacRole;

public class TestUserRoleDeserializer extends JsonDeserializer<RbacRole> {

    @Override
    public RbacRole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String name = p.getValueAsString();
        return StreamEx.of(RbacRole.values())
                .findAny(role -> RbacRole.toSource(role).getLiteral().equals(name))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid role name %s", name)));
    }

}
