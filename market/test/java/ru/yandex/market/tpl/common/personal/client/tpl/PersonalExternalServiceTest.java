package ru.yandex.market.tpl.common.personal.client.tpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalFindApi;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalRetrieveApi;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalStoreApi;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;

@RequiredArgsConstructor
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PersonalExternalService.class})
class PersonalExternalServiceTest {

    @Autowired
    private PersonalExternalService personalExternalService;
    @MockBean
    private DefaultPersonalRetrieveApi personalRetrieveApi;
    @MockBean
    private DefaultPersonalStoreApi personalStoreApi;
    @MockBean
    private DefaultPersonalFindApi personalFindApi;

    @BeforeEach
    public void setUp() {
        Mockito.reset(personalRetrieveApi);
        Mockito.when(personalRetrieveApi.v1MultiTypesRetrievePost(any())).thenReturn(
                new PersonalMultiTypeRetrieveResponse().items(List.of(
                        new MultiTypeRetrieveResponseItem().id("example").value(new CommonType().phone(
                                "example"))
                        )
                )
        );
    }

    @Test
    public void getPersonalWithSizeLessThan1000() {
        personalExternalService.getMultiTypePersonalByIds(List.of(Pair.of("example", CommonTypeEnum.PHONE)));
        Mockito.verify(personalRetrieveApi, Mockito.times(1))
                .v1MultiTypesRetrievePost(any());
    }

    @Test
    public void getPersonalWithSizeMoreThan1000() {
        List<Pair<String, CommonTypeEnum>> ids = new ArrayList<>();
        for (int i = 0; i <= 1000; i++) {
            ids.add(Pair.of(String.valueOf(i), CommonTypeEnum.EMAIL));
        }
        Assertions.assertThat(ids.size()).isEqualTo(1001);
        personalExternalService.getMultiTypePersonalByIds(ids);
        Mockito.verify(personalRetrieveApi, Mockito.times(2))
                .v1MultiTypesRetrievePost(any());
    }

    @ParameterizedTest
    @MethodSource(value = "fullNames")
    void test(FullName fullName, String response) {
        Mockito.when(personalRetrieveApi.v1MultiTypesRetrievePost(any())).thenReturn(
                new PersonalMultiTypeRetrieveResponse().items(List.of(
                        new MultiTypeRetrieveResponseItem().id("example").value(new CommonType().fullName(fullName))
                        )
                )
        );

        var res = personalExternalService.getPersonalById("example", CommonTypeEnum.FULL_NAME);

        assertThat(res).isPresent();
        Optional<String> name = PersonalMapper.mapResponseItemToNameString(res.get());
        assertThat(name).isPresent();
        assertThat(name.get()).isEqualTo(response);
    }

    static Stream<Arguments> fullNames() {
        return Stream.of(
                Arguments.of(new FullName().forename("forename"), "forename"),
                Arguments.of(new FullName().forename("forename").surname("surname"), "surname forename"),
                Arguments.of(new FullName().forename("forename").surname("surname").patronymic("patrynomic"),
                        "surname forename patrynomic"),
                Arguments.of(new FullName().forename("forename").patronymic("patrynomic"), "forename patrynomic")
        );
    }

}
