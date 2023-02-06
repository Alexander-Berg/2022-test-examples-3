package ru.yandex.mail.cerberus.yt.staff;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.micronaut.test.annotation.MicronautTest;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.mail.cerberus.GroupId;
import ru.yandex.mail.cerberus.yt.staff.client.StaffResult;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffDepartmentGroup;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffRoom;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;
import ru.yandex.mail.micronaut.common.Page;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class DefaultStaffManagerTest {
    @Inject
    private ObjectMapper objectMapper;

    @Test
    void personDeserialization() throws IOException, ExecutionException, InterruptedException {
        final ObjectReader userReader = objectMapper.readerFor(StaffUser.class);
        final ObjectReader entityIdReader = objectMapper.readerFor(StaffEntityId.class);

        final String text = readJson("/staff/person.json");
        final StaffResult staffResult = objectMapper.readValue(text, StaffResult.class);

        final CompletableFuture<Page<Long, StaffEntity<StaffUser>>> future =
                DefaultStaffManager.doRequest(1, () -> CompletableFuture.completedFuture(staffResult),
                        DefaultStaffManager.asConverter(node -> parseObject(userReader, node),
                                entityIdReader));
        final Page<Long, StaffEntity<StaffUser>> longStaffEntityPage = future.get();
        final List<StaffEntity<StaffUser>> elements = longStaffEntityPage.getElements();
        assertThat(elements).isNotEmpty();
        for (StaffEntity<StaffUser> element : elements) {
            final StaffUser entity = element.getEntity();
            assertThat(entity.getGroups().size()).isEqualTo(79);
            final StreamEx<GroupId> groupIds = entity.departmentIds();
            final List<GroupId> groupIdsList = groupIds.toList();
            assertThat(groupIdsList.size()).isEqualTo(81);
        }
    }

    @Test
    void groupDeserialization() throws IOException, ExecutionException, InterruptedException {
        final ObjectReader groupReader = objectMapper.readerFor(StaffDepartmentGroup.class);
        final ObjectReader entityIdReader = objectMapper.readerFor(StaffEntityId.class);

        final String text = readJson("/staff/group.json");
        final StaffResult staffResult = objectMapper.readValue(text, StaffResult.class);

        final CompletableFuture<Page<Long, StaffEntity<StaffDepartmentGroup>>> future =
                DefaultStaffManager.doRequest(1, () -> CompletableFuture.completedFuture(staffResult),
                        DefaultStaffManager.asConverter(node -> parseObject(groupReader, node),
                                entityIdReader));
        final Page<Long, StaffEntity<StaffDepartmentGroup>> longStaffEntityPage = future.get();
        final List<StaffEntity<StaffDepartmentGroup>> elements = longStaffEntityPage.getElements();
        assertThat(elements).isNotEmpty();
        assertThat(elements.size()).isEqualTo(2);

        final StaffDepartmentGroup abcGroup = elements.get(0).getEntity();
        assertThat(abcGroup.getUrl()).isEqualTo("svc_stroganovcoworking");
        assertThat(abcGroup.getType()).isEqualTo(StaffDepartmentGroup.Types.SERVICE);
        assertThat(abcGroup.getUniqueId()).isEqualTo(188238);
        assertThat(abcGroup.getDepartment().getName()).isNull();
        assertThat(abcGroup.isDeleted()).isFalse();

        final StaffDepartmentGroup staffGroup = elements.get(1).getEntity();
        assertThat(staffGroup.getUrl()).isEqualTo("yandex_rkub_mobdev_pers_dep47221");
        assertThat(staffGroup.getType()).isEqualTo(StaffDepartmentGroup.Types.DEPARTMENT);
        assertThat(staffGroup.getUniqueId()).isEqualTo(115248);
        assertThat(staffGroup.getDepartment().getName()).isNotNull();
        assertThat(staffGroup.isDeleted()).isFalse();
    }

    @Test
    void roomsDeserialization() throws IOException, ExecutionException, InterruptedException {
        final ObjectReader userReader = objectMapper.readerFor(StaffRoom.class);
        final ObjectReader entityIdReader = objectMapper.readerFor(StaffEntityId.class);

        final String text = readJson("/staff/rooms.json");
        final StaffResult staffResult = objectMapper.readValue(text, StaffResult.class);

        final CompletableFuture<Page<Long, StaffEntity<StaffRoom>>> future =
                DefaultStaffManager.doRequest(1, () -> CompletableFuture.completedFuture(staffResult),
                        DefaultStaffManager.asConverter(node -> parseObject(userReader, node),
                                entityIdReader));
        final Page<Long, StaffEntity<StaffRoom>> longStaffEntityPage = future.get();
        final List<StaffEntity<StaffRoom>> elements = longStaffEntityPage.getElements();
        assertThat(elements).isNotEmpty();
        for (StaffEntity<StaffRoom> element : elements) {
            final StaffRoom entity = element.getEntity();
            assertThat(entity.getId().getValue()).isEqualTo(4713L);
        }
    }

    @SneakyThrows
    private static <T> T parseObject(ObjectReader reader, JsonNode json) {
        return reader.readValue(json);
    }


    @NotNull
    private String readJson(String path) throws IOException {
        final LineNumberReader src =
                new LineNumberReader(new InputStreamReader(DefaultStaffManagerTest.class.getResourceAsStream(
                        path), StandardCharsets.UTF_8));
        String line;
        StringBuilder text = new StringBuilder();
        while ((line = src.readLine()) != null) {
            text.append(line).append("\n");
        }
        return text.toString();
    }
}
