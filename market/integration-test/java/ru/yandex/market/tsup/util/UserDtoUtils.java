package ru.yandex.market.tsup.util;

import java.util.List;

import lombok.experimental.UtilityClass;

import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfUserDto;
import ru.yandex.mj.generated.client.carrier.model.UserDto;
import ru.yandex.mj.generated.client.carrier.model.UserSourceDto;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class UserDtoUtils {

    public static UserDto userDto(Long id, String firstName, String lastName) {
        return userDto(id, "+79998887766", firstName, lastName, null, UserSourceDto.CARRIER, List.of(company()));
    }

    public static UserDto userDto(Long id,
                                  String phone,
                                  String firstName,
                                  String lastName,
                                  String patronymic,
                                  UserSourceDto source,
                                  List<CompanyDto> companies) {
        return new UserDto().id(id)
                            .phone(phone)
                            .name(firstName + " " + lastName)
                            .firstName(firstName)
                            .lastName(lastName)
                            .patronymic(patronymic)
                            .source(source)
                            .companies(companies);
    }

    public static PageOfUserDto page(UserDto... users) {
        return page(List.of(users));
    }

    public static PageOfUserDto page(List<UserDto> users) {
        return new PageOfUserDto()
                .totalPages(1)
                .totalElements(2L)
                .size(10)
                .number(0)
                .content(users);
    }

    public static CompanyDto company() {
        return new CompanyDto()
                .id(1L)
                .deliveryServiceId(1L)
                .name("generic");
    }
}
