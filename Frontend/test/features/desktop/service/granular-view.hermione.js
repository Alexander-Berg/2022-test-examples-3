const PO = require('./PO');

async function assertServiceView(user, lang) {
    return this.browser
        .openIntranetPage({
            pathname: '/services/serviceforrobot003/',
            query: { lang },
        }, {
            user,
        })
        .waitForVisible(PO.service(), 10000)
        .waitForVisible(PO.serviceManagers())
        .waitForVisible(PO.serviceManagers.spin(), 5000, true)
        .waitForVisible(PO.operationalLink.spin(), 5000, true)
        .waitForVisible(PO.service.onDuty.loading(), 5000, true)
        .waitForVisible(PO.serviceTeamTitle.spin(), 5000, true)
        .waitForVisible(PO.service.onDuty.cutRedrawPending(), 5000, true)
        .assertView(`service-${lang}-for-${user}`, PO.service(), {
            animationDisabled: true,
            hideElements: ['.YndxBug', '.tools-lamp'],
            redrawElements: ['.Image_loading'],
            ignoreElements: ['.person__userpic', '.Employee-Avatar'],
        });
}

async function assertErrorView(user, lang) {
    return this.browser
        .openIntranetPage({
            pathname: '/services/robotinternal003service/',
            query: { lang },
        }, {
            user,
        })
        .waitForVisible(PO.bpage.content(), 10000)
        .waitForVisible(PO.service.onDuty.loading(), 10000, true)
        .assertView(`error-${lang}-for-${user}`, PO.bpage.content());
}

const users = [
    'robot-abc-002',
    'robot-abc-003',
    'robot-abc-004',
];

describe('Просмотр информации о сервисе', () => {
    describe('Внешний вид страницы сервиса', function() {
        for (const lang of ['ru', 'en']) {
            for (const user of users) {
                it(`(${lang}) Для юзера ${user}`, async function() {
                    return assertServiceView.call(this, user, lang);
                });
            }
        }

        for (const lang of ['ru', 'en']) {
            it(`(${lang}) Для юзера robot-abc-004 - 403 доступ запрещен`, function() {
                return assertErrorView.call(this, 'robot-abc-004', lang);
            });
        }
    });
});
