package ru.yandex.canvas.service;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.canvas.model.direct.Privileges;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

/**
 * Created by solovyev on 12.07.17.
 */
public class PrivilegesTest {

    @Test
    public void serialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String privilegesStr =
                objectMapper.writeValueAsString(new Privileges(Arrays.asList(Privileges.Permission.CREATIVE_CREATE)));
        Privileges privileges = objectMapper.readValue(privilegesStr, Privileges.class);
        Assert.assertNotNull(privileges);
    }

    @Test
    public void unknownPermission() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Privileges privileges = objectMapper.readValue(
                "{\"permissions\":[\"creative_create\",\"internal_user\"]}", Privileges.class);
        assertThat(privileges.getPermissions(), hasItems(Privileges.Permission.CREATIVE_CREATE));
        assertThat(privileges.getPermissions(), hasItems(Privileges.Permission.UNKNOWN_PERMISSION));
    }
}
