'use strict';

const PO = require('../../page-objects/desktop').PO;

specs('Подвал', function() {
    hermione.only.notIn('ie8', 'nth-of-type не работает в ie8-');
    it('Проверка ссылок', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', exp_flags: 'hide-popups=1' })
            .yaCheckLink(PO.footer.firstLine.settingsLink()).then(url => {
                return this.browser.yaCheckURL(url, { pathname: '/tune/search' },
                    'Сломана ссылка «Настройки»',
                    { skipProtocol: true, skipHostname: true, skipQuery: true }
                );
            })
            .yaCheckBaobabCounter(PO.footer.firstLine.settingsLink(), { path: '/$page/$footer/link[@type="settings"]' })
            .yaCheckLink(PO.footer.firstLine.appLink()).then(url => {
                return this.browser.yaCheckURL(url, { hostname: 'mobile.yandex.ru' },
                    'Сломана ссылка «Приложения»',
                    { skipProtocol: true, skipPathname: true, skipQuery: true }
                );
            })
            .yaCheckBaobabCounter(PO.footer.firstLine.appLink(), { path: '/$page/$footer/link[@type="mobile"]' })
            .yaCheckLink(PO.footer.firstLine.feedbackLink()).then(url => {
                return this.browser.yaCheckURL(url,
                    { hostname: 'yandex.ru', pathname: '/support/search/troubleshooting/feedback.html' },
                    'Сломана ссылка «Обратная связь»',
                    { skipProtocol: true, skipQuery: true }
                );
            })
            .yaCheckBaobabCounter(PO.footer.firstLine.feedbackLink(), { path: '/$page/$footer/link[@type="feedback"]' })
            .yaCheckLink(PO.footer.firstLine.helpLink()).then(url => {
                return this.browser.yaCheckURL(url,
                    { hostname: 'yandex.ru', pathname: '/support/search/' },
                    'Сломана ссылка «Справка»',
                    { skipProtocol: true, skipQuery: true }
                );
            })
            .yaCheckBaobabCounter(PO.footer.firstLine.helpLink(), { path: '/$page/$footer/link[@type="help"]' })
            .yaCheckLink(PO.footer.secondLine.advLink()).then(url => {
                return this.browser.yaCheckURL(url,
                    { hostname: 'yandex.ru', pathname: '/adv/', query: { from: 'serp-footer' } },
                    'Сломана ссылка «Реклама»',
                    { skipProtocol: true }
                );
            })
            .yaCheckBaobabCounter(PO.footer.secondLine.advLink(), { path: '/$page/$footer/link[@type="ad"]' })
            .yaCheckLink(PO.footer.secondLine.statLink()).then(url => {
                return this.browser.yaCheckURL(url,
                    { hostname: 'radar.yandex.ru' },
                    'Сломана ссылка «Статистика»',
                    { skipProtocol: true, skipPathname: true, skipQuery: true }
                );
            })
            .yaCheckBaobabCounter(PO.footer.secondLine.statLink(), { path: '/$page/$footer/link[@type="stat"]' })
            .yaCheckLink(PO.footer.secondLine.licenseLink()).then(url => {
                return this.browser.yaCheckURL(url,
                    { hostname: 'yandex.ru', pathname: '/legal/termsofuse/' },
                    'Сломана ссылка «Лицензия на поиск»',
                    { skipProtocol: true, skipQuery: true }
                );
            })
            .yaCheckBaobabCounter(PO.footer.secondLine.licenseLink(), { path: '/$page/$footer/link[@type="license"]' })
            .yaCheckLink(PO.footer.secondLine.aboutLink()).then(url => {
                return this.browser.yaCheckURL(url, { hostname: 'company.yandex.ru' },
                    'Сломана ссылка «О компании»',
                    { skipProtocol: true, skipPathname: true, skipQuery: true }
                );
            })
            .yaCheckBaobabCounter(PO.footer.secondLine.aboutLink(), { path: '/$page/$footer/link[@type="about"]' });
    });
});
