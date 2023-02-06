module.exports = {
    // Смена Пароля Администратора
    changeAdminPasswordOrgAdmin: {
        login: 'didier.morgan@yandex.ru',
    },
    domainAdmin: {
        login: 'test3@test1.connect-test.tk',
    },
    alex: {
        login: 'admin@ui-test.yaconnect.com',
        uid: 1130000001116414,
        orgId: 30,
    },
    chuck: {
        login: 'chuck2@ui-test.yaconnect.com',
    },
    externalAdmin: {
        login: 'external-adm@yandex.ru',
    },
    externalDeputyAdmin: {
        login: 'deputy-adm@yandex.ru',
    },
    // оплаченный расширенный коннект
    // владелец организации
    // есть неподтвержденный домен
    adminUITest1: {
        login: 'create-user@ui-test1.yaconnect.com',
        uid: 1130000001190474,
        orgId: 32,
    },
    // Включен трекер, есть лицензии
    adminUITest2: {
        login: 'admin@ui-test2.yaconnect.com',
        uid: 1130000001197666,
        orgId: 33,
    },
    // админ организации без домена (яорг)
    jane: {
        login: 'jane-smith@yandex.ru',
        nickname: 'jane.smith',
        uid: 4020693992,
        orgId: 100255,
    },
    // админ в орг с задолженностью
    debtor: {
        login: 'deptor@yandex.ru',
        uid: 4022129056,
        orgId: 100905,
    },
    // админ в яорг без домена
    adminYO1: {
        login: 'UITesterConnect1',
    },
    // админ в яорг с неподтвержденным доменом и с подтвержденным
    // включенный трекер, не заполнена платежная инфа
    adminYO2: {
        login: 'UITesterConnect2',
        uid: 4024057344,
        orgId: 101400,
    },
    // пользователь изначально без организации, для тестирования её создания
    // при перегенерации дампов - придется пересоздать
    createOrg: {
        login: 'create-org-ui-connect21',
    },
    KimberlyMcGregor: {
        login: 'kimberly.mcgregor@cap.auto.connect-test.tk',
    },
    // Обычный пользователь в организации с включенным трекером с подписками
    simpleUser: {
        login: 'simpleuser@ui-test2.yaconnect.com',
    },
    // Админ трекера с 6 доменными пользователями и заполненной платежной инфой
    trackerAdmin: {
        login: 'uiconnecttracker@yandex.ru',
        uid: 4029878320,
        orgId: 102607,
    },
    // Включен трекер, не заполнена платежная инфа
    adminYO3: {
        login: 'auto-a',
    },
    // Админ организации в которой нет подписок трекера, трекер включен
    serviceAdmin: {
        login: 'uitesterconnect3',
    },
    // Пользователь для проверки невозможности выключения сервисов
    serviceUser: {
        login: 'user@ui14.auto.connect-test.tk',
    },
    // Админ организации в которой есть подписки трекера, трекер включен
    serviceAdminWithSubs: {
        login: 'apod',
    },
    // админ организации для удаления без пользователями
    removedOrgWithoutUsersAdmin: {
        login: 'ui-test-trash-org-jack22@yandex.ru',
    },
    // админ организации для удаления с пользователями
    removedOrgWithUsersAdmin: {
        login: 'ui-test-trash-org-jane@yandex.ru',
    },
    // админ организации для удаления с ресурсами
    removedOrgWithResourcesAdmin: {
        login: 'ui-test-trash-org-bob@yandex.ru',
    },
    // админ организации для удаления с доменом
    removedOrgWithDomain: {
        login: 'ui-test-trash-org-jack25@yandex.ru',
    },
    removedOrgWithBillingInfo: {
        login: 'ui-test-trash-org-jack26',
    },
    // пользователь состоящий в нескольких организациях
    multi: {
        login: 'ui-multi@yandex.ru',
    },
    // админ для тестирования редактирования пользователя
    userEditTestAdmin: {
        login: 'ui-test-edit-user@yandex.ru',
    },
    sim: {
        login: 'sim@eee.yaconnect.com',
    },
    noOrgsUser: {
        login: 'ui-test-no-orgs@yandex.ru',
    },
};
