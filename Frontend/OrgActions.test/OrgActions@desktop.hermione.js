'use strict';

const POSimilar = require('../../SimilarCompanies/SimilarCompanies.test/SimilarCompanies.page-object/index@desktop');
const PO = require('./OrgActions.page-object').desktop;

specs({
    feature: 'Колдунщик 1орг',
    type: 'Кнопки',
}, function() {
    it('Ссылки и счетчики', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе брынза',
            data_filter: 'companies',
        }, PO.oneOrg());

        await browser.yaCheckLink2({
            selector: PO.oneOrg.buttons.delivery(),
            baobab: {
                path: '/$page/$parallel/$result/composite/tabs/about/actions/delivery[@action = "cta"]',
            },
            url: {
                href: 'https://eda.yandex/',
                ignore: ['protocol', 'pathname', 'query'],
            },
        });
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1394099338', event: 'order-food', goal: 'cta-button' },
            'Не сработала метрика на клик в кнопку заказать доставку',
        );

        await browser.yaCheckLink2({
            selector: PO.oneOrg.buttons.site(),
            baobab: {
                path: '/$page/$parallel/$result/composite/tabs/about/actions/site[@action = "site"]',
            },
            url: {
                href: 'https://vk.com/cafebrinza',
                ignore: ['protocol', 'pathname', 'query'],
            },
        });
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1394099338', event: 'site', goal: '' },
            'Не сработала метрика на клик в кнопку сайт',
        );

        await browser.yaCheckLink2({
            selector: PO.oneOrg.buttons.route(),
            baobab: {
                path: '/$page/$parallel/$result/composite/tabs/about/actions/route-button[@action = "route"]',
            },
            url: {
                href: 'https://yandex.ru/maps/2/saint-petersburg',
                ignore: ['protocol', 'pathname', 'query'],
            },
        });
        await browser.click(PO.oneOrg.buttons.route());
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1394099338', event: 'route', goal: 'make-route' },
            'Не сработала метрика на клик в кнопку маршрута',
        );

        await browser.yaCheckVacuum(
            { type: 'show', orgid: '1394099338', event: 'show_org' },
            'Не сработала метрика на показ организации',
        );
    });

    it('В попапе', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'бродвей кафе екатеринбург',
            data_filter: 'companies',
        }, PO.oneOrg());

        await browser.click(POSimilar.oneOrg.similarCompanies.scroller.firstItem());
        await browser.yaWaitForVisible(PO.modal.oneOrg(), 'попап с 1орг не открылся');
        await browser.yaWaitForVisible(PO.modal.oneOrg.buttons());

        await browser.click(PO.modal.oneOrg.buttons.delivery());
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1761083598', event: 'order-food', goal: 'cta-button' },
            'Не сработала метрика на клик в кнопку заказать доставку',
        );

        await browser.click(PO.modal.oneOrg.buttons.site());
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1761083598', event: 'site', goal: '' },
            'Не сработала метрика на клик в кнопку сайт',
        );

        await browser.yaCheckVacuum(
            { type: 'show', orgid: '1761083598', event: 'show_org' },
            'Не сработала метрика на показ организации',
        );

        await browser.click(PO.modal.oneOrg.buttons.route());
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1761083598', event: 'route', goal: 'make-route' },
            'Не сработала метрика на клик в кнопку маршрута',
        );

        await browser.click(PO.modal.oneOrg.tabAbout.contacts.PhoneItem.Link());
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1761083598', event: 'call', goal: 'make-call' },
            'Не сработала метрика на клик в телефон',
        );

        await browser.click(PO.modal.oneOrg.tabAbout.contacts.SiteItem.Link());
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1761083598', event: 'site', goal: '' },
            'Не сработала метрика на клик на сайт в фактах',
        );
    });

    it('Внешний вид в центре', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'Отдел МВД России по району Марьино города Москвы, Москва',
            oid: 'b:1040159081',
            data_filter: 'companies',
        }, PO.oneOrgLeft.buttons());

        await browser.assertView('plain', PO.oneOrgLeft.buttons());
    });

    it('Advert кнопка с ифреймом', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'детский сад курочка ряба',
            data_filter: 'companies',
        }, PO.oneOrg());

        await browser.yaCheckBaobabCounter(PO.oneOrg.buttons.advert(), {
            path: '$page/$parallel/$result/composite/tabs/about/actions/advert[@action = "cta"]',
        });

        await browser.yaWaitForVisible(PO.frame(), 'Не открылся ифрейм');

        const src = await browser.getAttribute(PO.frame.iframe(), 'src');

        assert.isDefined(src, 'Нет src у ифрейма');

        await browser.assertView('iframe', PO.frame(), {
            invisibleElements: [`body > *:not(${PO.frame()})`],
            ignoreElements: [PO.frame.iframe()],
        });
    });
});
