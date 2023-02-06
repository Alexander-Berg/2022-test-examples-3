package ru.yandex.autotests.direct.cmd.steps.user;

import java.util.Date;

import org.apache.xmlrpc.XmlRpcException;

import ru.yandex.autotests.balance.lib.environment.Environment;
import ru.yandex.autotests.balance.lib.xmlrpc.UnitTestLib;
import ru.yandex.autotests.direct.utils.clients.tus.TusClient;
import ru.yandex.autotests.direct.utils.clients.tus.TusCreateAccountResponse;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.model.User;

public class UserExternalServicesSteps {

    private static final int SERVICE_ID = 7; // ID сервиса (7=Директ, 11=Маркет и т.д.)
    private static UnitTestLib unitTestLib = new UnitTestLib(Environment.valueOf(
            DirectTestRunProperties.getInstance().getBalanceStageType()));
    private static UserExternalServicesSteps instance = null;

    public static UserExternalServicesSteps getInstance() {
        if (instance == null) {
            instance = new UserExternalServicesSteps();
        }
        return instance;
    }

    public static Integer createBalanceClientId(Integer region, String currency) {
        return createBalanceClientId(null, region, currency);
    }

    public static Integer createBalanceClientId(Integer agencyId, Integer region, String currency) {
        Integer clientId = null;
        try {
            clientId = unitTestLib.createClient(
                    null,
                    null,
                    agencyId,
                    region,
                    currency, new Date(1), SERVICE_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clientId;
    }

    public static Integer createBalanceFixedClientId(Integer agencyId, Integer region) {
        Integer clientId = null;
        try {
            clientId = unitTestLib.createClient(
                    null,
                    false,
                    agencyId,
                    region,
                    null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clientId;
    }

    public static void associateLogin(int clientId, String passportUIID) {
        try {
            unitTestLib.associateLogin(clientId, passportUIID);
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
    }

    private final TusClient tusClient = new TusClient();

    public User createBalanceClient(Integer region, String currency) {
        return createBalanceClient(null, region, currency);
    }

    public User createBalanceClient(Integer agencyId, Integer region, String currency) {
        Integer clientId = createBalanceClientId(agencyId, region, currency);
        User user = createNewPassportLogin();
        associateLogin(clientId, user.getPassportUID());
        return user;
    }

    public User createBalanceFixedClient(Integer agencyId, Integer region) {
        Integer clientId = createBalanceFixedClientId(agencyId, region);
        User user = createNewPassportLogin();
        associateLogin(clientId, user.getPassportUID());
        return user;
    }

    public User createNewPassportLogin() {
        TusCreateAccountResponse regUser = tusClient.createAccount();
        User user = new User();
        user.setLogin(regUser.getAccount().getLogin());
        user.setPassword(regUser.getAccount().getPassword());
        return user;
    }

}
