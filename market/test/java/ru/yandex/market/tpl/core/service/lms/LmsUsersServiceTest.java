package ru.yandex.market.tpl.core.service.lms;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.api.model.user.TplUserPropertyDto;
import ru.yandex.market.tpl.core.domain.lms.user.LmsUserFilterDto;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyEntity;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.service.lms.user.LmsUserSpecification;
import ru.yandex.market.tpl.core.service.lms.user.LmsUsersService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.ROUTING_TAGS;

@CoreTest
public class LmsUsersServiceTest {
    @MockBean
    UserRepository userRepository;

    @MockBean
    UserPropertyService userPropertyService;

    @Autowired
    LmsUsersService service;

    @Test
    public void shouldReturnUsersPropertiesGrid() {
        var pg = mock(Pageable.class);

        User user = UserUtil.of(55L, 1234L, "Ivan Ivanovich");

        UserPropertyEntity property = mock(UserPropertyEntity.class);
        when(property.getName()).thenReturn("hello");
        when(property.getValue()).thenReturn("world");
        when(property.getId()).thenReturn(Long.valueOf(55L));


        Page<UserPropertyEntity> pagedResponse = new PageImpl(List.of(property));

        when(userPropertyService.findByUserIdAndNameNotIn(any(Pageable.class), any(Long.class), any()))
                .thenReturn(pagedResponse);

        var resp = service.getUserProperties(pg, 55L);
        var items = resp.getItems();
        var props = items.get(0).getValues();

        Assertions.assertEquals(items.size(), 1);
        Assertions.assertEquals(items.get(0).getId(), 55L);

        Assertions.assertEquals(props.get("name"), "hello");
        Assertions.assertEquals(props.get("value"), "world");
    }

    @Test
    public void shouldReturnUsersPropertiesDetail() {
        User user = UserUtil.of(55L, 1234L, "Ivan Ivanovich");

        UserPropertyEntity property = mock(UserPropertyEntity.class);
        when(property.getName()).thenReturn("hello");
        when(property.getValue()).thenReturn("world");
        when(property.getId()).thenReturn(Long.valueOf(55L));
        when(property.getUser()).thenReturn(user);

        when(userPropertyService.findById(any()))
                .thenReturn(Optional.of(property));

        var resp = service.getProperty(55L);
        var item = resp.getItem();
        var props = item.getValues();

        Assertions.assertEquals(item.getId(), 55L);
        Assertions.assertEquals(props.get("name"), "hello");
        Assertions.assertEquals(props.get("value"), "world");
    }

    @Test
    public void shouldReturnUserGrid() {
        var filterDto = new LmsUserFilterDto();
        var pg = mock(Pageable.class);

        User user = UserUtil.of(55L, 1234L, "Ivan Ivanovich");

        Page<User> pagedResponse = new PageImpl(List.of(user));

        when(userRepository.findAll(any(LmsUserSpecification.class), any(Pageable.class)))
                .thenReturn(pagedResponse);
        var resp = service.getUsers(filterDto, pg);
        var items = resp.getItems();
        var props = items.get(0).getValues();

        Assertions.assertEquals(items.size(), 1);
        Assertions.assertEquals(items.get(0).getId(), 55L);

        Assertions.assertEquals((Long) props.get("uid"), 1234L);
        Assertions.assertEquals(props.get("name"), "Ivan Ivanovich");
    }

    @Test
    public void shouldReturnUserDetail() {
        User user = UserUtil.of(55L, 1234L, "Ivan Ivanovich");

        when(userRepository.findById(any()))
                .thenReturn(Optional.of(user));
        var resp = service.getUserById(55L);
        var item = resp.getItem();
        var props = item.getValues();

        Assertions.assertEquals(item.getId(), 55L);
        Assertions.assertEquals((Long) props.get("uid"), 1234L);
        Assertions.assertEquals(props.get("name"), "Ivan Ivanovich");
    }

    @Test
    public void shouldUpdateUser() {
        User user = UserUtil.of(55L, 1234L, "Ivan Ivanovich");

        when(userRepository.findById(any()))
                .thenReturn(Optional.of(user));
        UserPropertyEntity property = mock(UserPropertyEntity.class);
        when(property.getName()).thenReturn("hello");
        when(property.getValue()).thenReturn("world");
        when(property.getId()).thenReturn(Long.valueOf(55L));
        when(property.getUser()).thenReturn(user);
        when(userPropertyService.findById(any()))
                .thenReturn(Optional.of(property));
        property.setValue(ROUTING_TAGS.getName());
        when(userPropertyService.save(any()))
                .thenReturn(property);

        TplUserPropertyDto lmsUserPropertyDto = new TplUserPropertyDto();
        lmsUserPropertyDto.setId(65L);
        lmsUserPropertyDto.setName(ROUTING_TAGS.getName());
        lmsUserPropertyDto.setType(TplPropertyType.STRING.toString());
        lmsUserPropertyDto.setValue("prepaid");
        Assertions.assertDoesNotThrow(() -> service.updateProperty(lmsUserPropertyDto));
    }
}
