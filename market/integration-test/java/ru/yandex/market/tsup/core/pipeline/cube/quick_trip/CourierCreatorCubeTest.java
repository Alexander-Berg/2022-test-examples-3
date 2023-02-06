package ru.yandex.market.tsup.core.pipeline.cube.quick_trip;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.cube.CarrierCourierCreatorCube;
import ru.yandex.market.tsup.core.pipeline.data.quick_trip.CourierCreationData;
import ru.yandex.market.tsup.core.pipeline.data.quick_trip.CourierId;
import ru.yandex.mj.generated.client.carrier.api.UserApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfUserDto;
import ru.yandex.mj.generated.client.carrier.model.UserCompanyDto;
import ru.yandex.mj.generated.client.carrier.model.UserCreateDto;
import ru.yandex.mj.generated.client.carrier.model.UserDto;

public class CourierCreatorCubeTest extends AbstractContextualTest {
    @Autowired
    private CarrierCourierCreatorCube cube;

    @Autowired
    private UserApiClient userApiClient;

    @Test
    void testExisting() {
        CourierCreationData data = new CourierCreationData()
            .setCompanyId(1L)
            .setExistingCourierId(1L);

        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
            new PageOfUserDto()
                .content(List.of(new UserDto().id(1L).companies(List.of(new CompanyDto().id(1L)))))
                .size(1)
                .number(0)
                .totalElements(1L)
                .totalPages(1))
        );

        Mockito.when(
                userApiClient.internalUsersGet(1L, null, null, null, null, null, null, null, null, "id,DESC")
            )
            .thenReturn(call);

        var result = cube.execute(data);

        Mockito.verify(userApiClient, Mockito.times(0)).internalUsersCreatePost(Mockito.any());
        Mockito.verify(userApiClient, Mockito.times(0)).internalUsersAddCompanyPost(Mockito.any());

        softly.assertThat(result).isEqualTo(new CourierId(1L));
    }

    @Test
    void testExistingAddCompany() {
        CourierCreationData data = new CourierCreationData()
            .setCompanyId(1L)
            .setExistingCourierId(5L);

        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
            new PageOfUserDto()
                .content(List.of(new UserDto().id(5L).companies(List.of(new CompanyDto().id(2L)))))
                .size(1)
                .number(0)
                .totalElements(1L)
                .totalPages(1))
        );

        Mockito.when(
                userApiClient.internalUsersGet(5L, null, null, null, null, null, null, null, null, "id,DESC")
            )
            .thenReturn(call);

        var result = cube.execute(data);

        Mockito.verify(userApiClient, Mockito.times(0)).internalUsersCreatePost(Mockito.any());
        Mockito.verify(userApiClient, Mockito.times(1)).internalUsersAddCompanyPost(
            new UserCompanyDto().userId(5L).companyId(1L)
        );

        softly.assertThat(result).isEqualTo(new CourierId(5L));
    }

    @Test
    void testCreateNew() {
        CourierCreationData data = new CourierCreationData()
            .setCompanyId(1L)
            .setData(new CourierCreationData.CreationData()
                .setName("Иван")
                .setSurname("Иванов")
                .setPatronymic("Иванович")
                .setPhone("+71234567890")
            );

        ExecuteCall<UserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
            new UserDto().id(12L)
        ));

        Mockito.when(
                userApiClient.internalUsersCreatePost(
                    new UserCreateDto()
                        .companyId(1L)
                        .name("Иван")
                        .surname("Иванов")
                        .patronymic("Иванович")
                        .phone("+71234567890")
                )
            )
            .thenReturn(call);
        var result = cube.execute(data);

        softly.assertThat(result).isEqualTo(new CourierId(12L));
    }
}
