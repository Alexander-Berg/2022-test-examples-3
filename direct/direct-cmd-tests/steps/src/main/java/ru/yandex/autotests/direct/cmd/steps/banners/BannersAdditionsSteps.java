package ru.yandex.autotests.direct.cmd.steps.banners;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.banners.additions.DeleteBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.additions.DeleteBannersAdditionsResponse;
import ru.yandex.autotests.direct.cmd.data.banners.additions.GetBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.additions.GetBannersAdditionsResponse;
import ru.yandex.autotests.direct.cmd.data.banners.additions.RemoderateBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.additions.BannersAdditionsErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.banners.additions.SaveBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/*
* todo javadoc
*/
public class BannersAdditionsSteps extends DirectBackEndSteps {

    @Step("Получение текстовых дополнений для клиента {0} limit {1} offset {2}")
    public GetBannersAdditionsResponse getCallouts(String ulogin, Integer limit, Integer offset) {
        return getBannersAdditions(GetBannersAdditionsRequest.getDefaultCalloutsRequest(ulogin)
                .withLimit(limit)
                .withOffset(offset)
        );
    }

    @Step("Получение текстовых дополнений для клиента {0}")
    public List<String> getCalloutsList(String ulogin) {
        return getCallouts(ulogin).getCallouts()
                .stream()
                .map(Callout::getCalloutText)
                .collect(toList());
    }

    @Step("Получение текстовых дополнений для клиента {0}")
    public GetBannersAdditionsResponse getCallouts(String ulogin) {
        return getBannersAdditions(GetBannersAdditionsRequest.getDefaultCalloutsRequest(ulogin));
    }

    @Step("Получение дисклеймеров для клиента {0}")
    public GetBannersAdditionsResponse getDisclaimers(String ulogin) {
        return getBannersAdditions(GetBannersAdditionsRequest.getDefaultDisclaimerRequest(ulogin));
    }

    @Step("GET cmd = getBannersAdditions (Получение текстовых дополнений)")
    public GetBannersAdditionsResponse getBannersAdditions(GetBannersAdditionsRequest request) {
        return get(CMD.GET_BANNERS_ADDITIONS, request, GetBannersAdditionsResponse.class);
    }

    @Step("GET cmd = getBannersAdditions (Получение текстовых дополнений: ожидаем ошибку)")
    public ErrorResponse getBannersAdditionsError(GetBannersAdditionsRequest request) {
        return get(CMD.GET_BANNERS_ADDITIONS, request, ErrorResponse.class);
    }

    @Step("Сохранение текстовых дополнений для клиента {0}")
    public GetBannersAdditionsResponse saveBannersCallouts(String ulogin, List<String> callouts) {
        return saveBannersCallouts(ulogin, callouts.toArray(new String[callouts.size()]));
    }

    @Step("Сохранение текстовых дополнений {1} для клиента {0}")
    public List<Callout> saveBannersCalloutsSafe(String ulogin, String... callouts) {
        GetBannersAdditionsResponse response =
                saveBannersCallouts(ulogin, callouts);

        if(! "1".equals(response.getSuccess()) ) {
            throw new DirectCmdStepsException("сохранение прошло неудачно");
        }

        return Optional.ofNullable(response.getCallouts())
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет дополнений"));

    }

    @Step("Сохранение текстовых дополнений {1} для клиента {0}")
    public List<Callout> saveBannersCalloutsList(String ulogin, String... callouts) {
        GetBannersAdditionsResponse response =
                saveBannersAdditions(SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, callouts));
        return Optional.ofNullable(response.getCallouts())
                .orElseThrow(() -> new DirectCmdStepsException("Список сохраненных дополнений пуст"));
    }

    @Step("Сохранение текстовых дополнений {1} для клиента {0}")
    public GetBannersAdditionsResponse saveBannersCallouts(String ulogin, String... callouts) {
        return saveBannersAdditions(SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, callouts));
    }

    @Step("POST cmd = saveBannersAdditions (Сохранение текстовых дополнений)")
    public GetBannersAdditionsResponse saveBannersAdditions(SaveBannersAdditionsRequest request) {
        return post(CMD.SAVE_BANNERS_ADDITIONS, request, GetBannersAdditionsResponse.class);
    }

    @Step("POST cmd = saveBannersAdditions (Сохранение текстовых дополнений с ожиданием ошибки)")
    public BannersAdditionsErrorsResponse saveBannersAdditionsError(SaveBannersAdditionsRequest request) {
        return post(CMD.SAVE_BANNERS_ADDITIONS, request, BannersAdditionsErrorsResponse.class);
    }

    @Step("Удаление текстовых дополнений {1} для клиента {0}")
    public DeleteBannersAdditionsResponse deleteClientCalloutsSafe(String ulogin, Callout... callouts) {
        DeleteBannersAdditionsResponse response =
                deleteClientCallouts(DeleteBannersAdditionsRequest.defaultCalloutsRequest(ulogin, callouts));
        assumeThat("Удаление прошло успешно", response.getSuccess(), equalTo("1"));
        return response;
    }

    @Step("Удаление текстовых дополнений {1} для клиента {0}")
    public DeleteBannersAdditionsResponse deleteClientCallouts(String ulogin, Long... ids) {
        return deleteClientCallouts(DeleteBannersAdditionsRequest.defaultCalloutsRequest(ulogin, ids));
    }

    @Step("POST cmd = deleteBannersAdditions (Удаление текстовых дополнений)")
    public DeleteBannersAdditionsResponse deleteClientCallouts(DeleteBannersAdditionsRequest request) {
        return post(CMD.DELETE_BANNERS_ADDITIONS, request, DeleteBannersAdditionsResponse.class);
    }

    @Step("POST cmd = deleteBannersAdditions (Удаление текстовых дополнений: ожидаем ошибку)")
    public BannersAdditionsErrorsResponse deleteClientCalloutsError(DeleteBannersAdditionsRequest request) {
        return post(CMD.DELETE_BANNERS_ADDITIONS, request, BannersAdditionsErrorsResponse.class);
    }

    @Step("POST cmd = remoderateBannersAdditions (Перемодерация текстовых дополнений)")
    public DeleteBannersAdditionsResponse remoderateClientCallouts(RemoderateBannersAdditionsRequest request) {
        return post(CMD.REMODERATE_BANNERS_ADDITIONS, request, DeleteBannersAdditionsResponse.class);
    }

    @Step("POST cmd = moderateAcceptBannersAdditions (Перемодерация текстовых дополнений)")
    public DeleteBannersAdditionsResponse moderateAcceptClientCallouts(RemoderateBannersAdditionsRequest request) {
        return post(CMD.MODERATE_ACCEPT_BANNERS_ADDITIONS, request, DeleteBannersAdditionsResponse.class);
    }

    @Step("POST cmd = remoderateBannersAdditions (Перемодерация текстовых дополнений: ожидание ошибки)")
    public ErrorResponse remoderateClientCalloutsError(RemoderateBannersAdditionsRequest request) {
        return post(CMD.REMODERATE_BANNERS_ADDITIONS, request, ErrorResponse.class);
    }
}
