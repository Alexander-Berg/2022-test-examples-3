const PO = require('./PO');

describe('Команда сервиса', function() {
    it('Просмотр команды сервиса', function() {
        return this.browser
            // открыть страницу сервиса robotinternal003service (/services/robotinternal003service/)
            // под логином robot-internal-003
            .openIntranetPage({ pathname: '/services/robotinternal003service' })

            .waitForVisible(PO.serviceTeamScope())
            .waitForVisible(PO.serviceTeamHeadSpin(), 5000, true)

            .assertView('team-view', PO.serviceTeam());
    });

    it('Просмотр команды сервиса на английской версии', function() {
        return this.browser
            // открыть страницу сервиса robotinternal003service (/services/robotinternal003service/?lang=en)
            // под логином robot-internal-003
            .openIntranetPage({
                pathname: '/services/robotinternal003service',
                query: { lang: 'en' },
            })

            .waitForVisible(PO.serviceTeamScope())
            .waitForVisible(PO.serviceTeamHeadSpin(), 1000, true)

            .assertView('en-team-view', PO.serviceTeam());
    });
});
