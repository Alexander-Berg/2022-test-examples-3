'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs('Вёрстка в мобильном приложении', function() {
    it('Проверка видимости блоков на странице', function() {
        return this.browser
            .yaOpenSerp({ text: 'test' })
            .then(() => runCommonChecks(this))
            .yaShouldNotBeVisible(PO.footer2());
    });

    it('Проверка видимости блоков на странице при медленном соединении', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', user_connection: 'slow_connection=1' })
            .then(() => runCommonChecks(this))
            .yaShouldBeVisible(PO.footer2())
            .yaShouldBeVisible(PO.footer2.switcherLink())
            .yaShouldNotBeVisible(PO.footer2.enterLink())
            .yaShouldNotBeVisible(PO.footer2.allServicesLink())
            .yaShouldNotBeVisible(PO.footer2.feedbackLink());
    });

    it('Проверка видимости веб вертикалей', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', 'serp-web-verticals': 1 })
            .yaShouldBeVisible(PO.search.services())
            .assertView('page', PO.page(), {
                allowViewportOverflow: true,
                compositeImage: false,
                ignoreElements: PO.serpList()
            });
    });
});

function runCommonChecks(ctx) {
    return ctx.browser
        .yaWaitForVisible(PO.page())
        .yaShouldNotBeVisible(PO.search())
        .yaShouldNotBeVisible(PO.regionFooter())
        .yaShouldNotBeVisible(PO.searchengines());
}
