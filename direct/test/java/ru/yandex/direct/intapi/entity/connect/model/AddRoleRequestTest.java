package ru.yandex.direct.intapi.entity.connect.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.JsonUtils.fromJson;
import static ru.yandex.direct.utils.JsonUtils.toJson;

public class AddRoleRequestTest {
    @Test
    public void serialize_success() {
        AddRoleRequest obj =
                new AddRoleRequest()
                        .setId(1L)
                        .setPath("/direct/user/chief/")
                        .setFields(new RequestFields().setResourceId(123L))
                        .setOrgId(222L)
                        .setSubjectType(SubjectType.USER);
        String actual = toJson(obj);
        String expected = "{\"id\":1,\"path\":\"/direct/user/chief/\",\"fields\":{\"resource_id\":123}," +
                "\"org_id\":222,\"subject_type\":\"user\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void deserialize_success() {
        String input = "{\"id\":1,\"path\":\"/direct/user/chief/\",\"fields\":{\"resource_id\":123}," +
                "\"org_id\":222,\"subject_type\":\"user\"}";
        AddRoleRequest actual = fromJson(input, AddRoleRequest.class);
        AddRoleRequest expected =
                new AddRoleRequest()
                        .setId(1L)
                        .setPath("/direct/user/chief/")
                        .setFields(new RequestFields().setResourceId(123L))
                        .setOrgId(222L)
                        .setSubjectType(SubjectType.USER);
        assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
    }
}
