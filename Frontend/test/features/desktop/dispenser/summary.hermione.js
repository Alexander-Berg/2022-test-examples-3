const PO = require('./PO');

describe('Dispenser: Сводка по ресурсам', function() {
    describe('Просмотр сводки по ресурсам', function() {
        it('1. на русском языке', function() {
            return this.browser
                .openIntranetPage({
                    pathname: '/hardware/summary',
                }, { user: 'robot-abc-002' })
                .waitForVisible(PO.summary(), 5000)
                .assertView('summary-ru', PO.summary());
        });

        it('2. на английской языке', function() {
            return this.browser
                .openIntranetPage({
                    pathname: '/hardware/summary',
                    query: { lang: 'en' },
                }, { user: 'robot-abc-002' })
                .waitForVisible(PO.summary(), 5000)
                .assertView('summary-en', PO.summary());
        });
    });
});
