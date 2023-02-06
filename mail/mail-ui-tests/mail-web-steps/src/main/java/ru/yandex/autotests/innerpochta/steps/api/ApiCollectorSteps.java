package ru.yandex.autotests.innerpochta.steps.api;

import edu.emory.mathcs.backport.java.util.Arrays;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.collector.Collector;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

import static ru.yandex.autotests.innerpochta.api.collectors.CollectorsHandler.collectorsHandler;
import static ru.yandex.autotests.innerpochta.api.collectors.DoCollectorCreateHandler.doCollectorCreateHandler;
import static ru.yandex.autotests.innerpochta.api.collectors.DoCollectorRemoveHandler.doCollectorRemoveHandler;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;

/**
 * Created by mabelpines on 14.05.15.
 */
public class ApiCollectorSteps {

    public RestAssuredAuthRule auth;

    public ApiCollectorSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: collectors. Получаем список всех коллекторов пользователя.")
    public List<Collector> getAllCollectors() {
        return Arrays.asList(collectorsHandler().withAuth(auth).withCollectrosTab()
                .callCollectorsHandler().then().extract().jsonPath(getJsonPathConfig())
                .getObject("models[0].data", Collector[].class));
    }

    @Step("Удаляем все пользовательские коллекторы.")
    public ApiCollectorSteps removeAllUserCollectors(){
        for(Collector collector : getAllCollectors()){
            removeCollector(collector.getEmail(), collector.getPopid());
        }
        return this;
    }

    @Step("Вызов api-метода: do-remove-collectors. Удаляем коллектор: “{0}“.")
    public ApiCollectorSteps removeCollector(String collectorEmail, String popid){
        if ((collectorEmail != null) || (popid != null))
            doCollectorRemoveHandler().withAuth(auth).withEmail(collectorEmail).withPopid(popid)
                .callDoCollectorRemoveHandler();
        return this;
    }

    @Step("Вызов api-метода: do-collector-create. Создаем новый коллектор для адреса “{0}“")
    public void createNewCollector(String email, String pwd, String server){
        doCollectorCreateHandler().withAuth(auth).withCopyFoldersParam(STATUS_ON).withEmailAsLogin(email)
                .withNoDeleteMsgParam(STATUS_ON).withPassword(pwd).withDefaultPort().withImapProtocol()
                .withServer(server).useSSL(STATUS_ON).callDoCollectorCreateHandler();
    }
}
