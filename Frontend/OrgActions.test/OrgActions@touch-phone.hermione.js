'use strict';

const PO = require('./OrgActions.page-object').touchPhone;

specs({
    feature: 'Одна организация',
    experiment: 'Блок кнопок',
}, function() {
    async function openSerp(browser, params = {}) {
        await browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
            ...params,
        }, PO.oneOrg());
        await browser.yaShouldBeVisible(PO.oneOrg.OrgActions(), 'Нет блока с кнопками');
    }

    describe('Основные проверки', function() {
        it('Внешний вид', async function() {
            const { browser } = this;

            await openSerp(browser);
            await browser.assertView('plain', PO.oneOrg.OrgActions());
        });

        hermione.only.notIn(['iphone'], 'orientation is not supported');
        it('Внешний вид в альбомной ориентации', async function() {
            const { browser } = this;

            await openSerp(browser);
            await browser.setOrientation('landscape');
            await browser.assertView('landscape', PO.oneOrg.OrgActions());
        });
    });

    describe('Кнопка связаться', function() {
        it('Один телефон', async function() {
            const { browser } = this;

            await openSerp(browser);
            await browser.assertView('phones', PO.oneOrg.OrgActions.Contacts());

            await browser.yaCheckVacuum(
                { type: 'show', orgid: '1018907821', event: 'show_org' },
                'Не сработала метрика на показ организации',
            );

            await browser.yaCheckBaobabCounter(PO.oneOrg.OrgActions.Contacts.Button(), {
                path: '/$page/$main/$result/composite/org-actions/scroller/contacts[@action="phone" and @behaviour@type="dynamic"]',
            });

            await browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '1018907821', event: 'call', goal: 'make-call' },
                'Не сработала метрика на клик в кнопку телефон',
            );
        });
    });

    describe('Кнопка сайты', function() {
        it('Только сайт', async function() {
            const { browser } = this;

            await openSerp(browser, { text: 'иридиум самсунг' });
            await browser.assertView('sites', PO.oneOrg.OrgActions.Sites());
            await browser.yaCheckBaobabCounter(PO.oneOrg.OrgActions.Sites.Button(), {
                path: '/$page/$main/$result/composite/org-actions/scroller/sites[@action = "site"]',
            });

            await browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '1154409442', event: 'site', goal: '' },
                'Не сработала метрика на клик в кнопку сайт',
            );
        });
    });

    it('Кнопка маршрут', async function() {
        const { browser } = this;

        await openSerp(browser);
        await browser.yaShouldBeVisible(PO.oneOrg.OrgActions.Route(), 'Нет кнопки');

        // скринридер должен читать подпись к ссылке-иконке, а не тексту рядом
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Route.Text(), 'aria-hidden', 'true');
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Route.Button(), 'aria-label', 'Маршрут');

        await browser.yaCheckBaobabCounter(
            PO.oneOrg.OrgActions.Route.Button(),
            {
                path: '/$page/$main/$result/composite/org-actions/scroller/route[@action="route" and @behaviour@type="dynamic"]',
            },
            'Не сработал счётчик на кнопке',
        );
        await browser.yaWaitForVisible(PO.overlayPanel.overlayIframe(), 'Оверлей не показался');

        const iframeSrc = await browser.getAttribute(PO.overlayPanel.overlayIframe(), 'src');
        await browser.yaCheckURL(
            iframeSrc,
            {
                url: 'https://yandex.ru/web-maps',
                queryValidator: query => (
                    query.mode === 'routes' &&
                    query.rtt === 'auto' &&
                    query['no-header'] === '1' &&
                    query['no-distribution'] === '1'
                ),
            },
            {
                skipProtocol: true,
            },
        );

        await browser.click(PO.overlayPanel.back());
        await browser.yaWaitForHidden(PO.overlayPanel(), 'Оверлей не закрылся');
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1018907821', event: 'route', goal: 'make-route' },
            'Не сработала метрика на клик в кнопку маршрута',
        );
    });

    it('Кнопка еда', async function() {
        const { browser } = this;

        await openSerp(browser);
        await this.browser.execute(function(selector) {
            document.querySelector(selector).scrollLeft = 1000;
        }, PO.oneOrg.OrgActions.Scroller.wrap());

        // скринридер должен читать подпись к ссылке-иконке, а не тексту рядом
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Delivery.Text(), 'aria-hidden', 'true');
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Delivery.Button(), 'aria-label', 'Доставка');

        await browser.assertView('eda', PO.oneOrg.OrgActions.Delivery());
        await browser.yaCheckBaobabCounter(PO.oneOrg.OrgActions.Delivery.Button(), {
            path: '/$page/$main/$result/composite/org-actions/scroller/eda[@action="cta" and @behaviour@type="dynamic"]',
        });

        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1018907821', event: 'order-food', goal: 'cta-button' },
            'Не сработала метрика на клик в кнопку доставки',
        );
    });

    it('Кнопка доставка', async function() {
        const { browser } = this;

        await openSerp(browser, { text: 'папа джонс' });
        await this.browser.execute(function(selector) {
            document.querySelector(selector).scrollLeft = 1000;
        }, PO.oneOrg.OrgActions.Scroller.wrap());

        // скринридер должен читать подпись к ссылке-иконке, а не тексту рядом
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Delivery.Text(), 'aria-hidden', 'true');
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Delivery.Button(), 'aria-label', 'Доставка');

        await browser.assertView('delivery', PO.oneOrg.OrgActions.Delivery());
        await browser.yaCheckBaobabCounter(PO.oneOrg.OrgActions.Delivery.Button(), {
            path: '/$page/$main/$result/composite/org-actions/scroller/delivery[@action="cta" and @behaviour@type="dynamic"]',
        });

        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '8206618384', event: 'order-food', goal: 'cta-button' },
            'Не сработала метрика на клик в кнопку доставки',
        );
    });

    it('Кнопка запись', async function() {
        const { browser } = this;

        await openSerp(browser, { text: 'кафе пушкин' });
        await browser.yaScrollContainer(PO.oneOrg.OrgActions.Scroller.wrap(), 1000);

        // скринридер должен читать подпись к ссылке-иконке, а не тексту рядом
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Booking.Text(), 'aria-hidden', 'true');
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Booking.Button(), 'aria-label', 'Запись');

        await browser.assertView('booking', PO.oneOrg.OrgActions.Booking());
        await browser.yaCheckBaobabCounter(PO.oneOrg.OrgActions.Booking.Button(), {
            path: '/$page/$main/$result/composite/org-actions/scroller/booking[@action="cta" and @behaviour@type="dynamic"]',
        });

        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1018907821', event: 'book-service', goal: 'cta-button' },
            'Не сработала метрика на клик в кнопку записи',
        );
    });

    it('Кнопка запись через yclients', async function() {
        const { browser } = this;

        await openSerp(browser, {
            text: 'салон красоты palmier',
            exp_flags: 'GEO_1org_booking_touch=1',
        });
        await browser.yaScrollContainer(PO.oneOrg.OrgActions.Scroller.wrap(), 1000);

        // скринридер должен читать подпись к ссылке-иконке, а не тексту рядом
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Booking.Text(), 'aria-hidden', 'true');
        await browser.yaCheckAttribute(PO.oneOrg.OrgActions.Booking.Button(), 'aria-label', 'Запись');

        await browser.assertView('booking', PO.oneOrg.OrgActions.Booking());
        await browser.yaCheckBaobabCounter(PO.oneOrg.OrgActions.Booking.Button(), {
            path: '/$page/$main/$result/composite/org-actions/scroller/booking[@action="cta" and @behaviour@type="dynamic"]',
        });

        await browser.yaWaitForVisible(PO.BookingIframe(), 'Шторка с формой бронирования через yclients не открылась');

        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '24044822785', event: 'book-service', goal: 'cta-button' },
            'Не сработала метрика на клик в кнопку записи',
        );
    });

    describe('Специальная кнопка', function() {
        it('В недвижимости', async function() {
            const { browser } = this;

            await openSerp(browser, { text: 'жк бунинские луга' });
            await browser.assertView('action', PO.oneOrg.OrgActions.Action());

            await browser.yaCheckVacuum(
                { type: 'show', orgid: '109944240168', event: 'show_org' },
                'Не сработала метрика на показ организации',
            );

            // проверяем, что до клика не вызвался счётчик – такое может быть, если налажать с loadable
            await browser.yaCheckRealtyCounter({ expected: 0 });
            await browser.yaCheckBaobabCounter(PO.oneOrg.OrgActions.Action(), {
                path: '/$page/$main/$result/composite/org-actions/realty',
                behaviour: { type: 'dynamic' },
            });
            await browser.yaWaitForVisible(PO.RealtyPopup());
            // После клика должен быть один запрос в апи недвижки
            await browser.yaCheckRealtyCounter();
            await browser.assertView('realty-popup', PO.RealtyPopup.Content());

            await browser.click(PO.RealtyPopup.Phone());
            await browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '109944240168', event: 'call', goal: 'make-call' },
                'Не сработала метрика на клик в кнопку телефон',
            );
        });
    });

    describe('В сайдблоке', function() {
        beforeEach(async function() {
            const { browser } = this;

            await openSerp(browser);
            await browser.yaOpenOverlayAjax(
                () => browser.click(PO.oneOrg.tabsMenu.about()),
                PO.overlayOneOrg(),
                'Сайдблок с карточкой организации не появился',
            );
        });

        it('Кнопка Позвонить', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(PO.overlayOneOrg.OrgActions.Contacts.Button(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/org-actions/scroller/contacts[@action = "phone"]',
            });
            await browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '1018907821', event: 'call', goal: 'make-call' },
                'Не сработала метрика на клик в телефон',
            );
        });

        it('Кнопка Сайт', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(PO.overlayOneOrg.OrgActions.Sites.Button(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/org-actions/scroller/sites [@action = "site"]',
            });
            await browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '1018907821', event: 'site', goal: '' },
                'Не сработала метрика на клик в сайт',
            );
        });

        it('Кнопка Доставки', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(PO.overlayOneOrg.OrgActions.Delivery.Button(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/org-actions/scroller/eda[@action="cta" and @behaviour@type="dynamic"]',
            });
            await browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '1018907821', event: 'order-food', goal: 'cta-button' },
                'Не сработала метрика на клик в доставку',
            );
        });

        it('Кнопка Маршрута', async function() {
            const { browser } = this;

            await browser.yaCheckVacuum(
                { type: 'show', orgid: '1018907821', event: 'show_org' },
                'Не сработала метрика на показ организации',
            );

            await browser.yaCheckBaobabCounter(PO.overlayOneOrg.OrgActions.Route.Button(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/org-actions/scroller/route[@action="route" and @behaviour@type="dynamic"]',
            });
            await browser.yaWaitForVisible(PO.overlayPanel.overlayIframe(), 'Оверлей не показался');
            const iframeSrc = await browser.getAttribute(PO.overlayPanel.overlayIframe(), 'src');
            await browser.yaCheckURL(
                iframeSrc,
                {
                    url: 'https://yandex.ru/web-maps',
                    queryValidator: query => (
                        query.mode === 'routes' &&
                        query.rtt === 'auto' &&
                        query['no-header'] === '1' &&
                        query['no-distribution'] === '1'
                    ),
                },
                { skipProtocol: true },
            );

            await browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '1018907821', event: 'route', goal: 'make-route' },
                'Не сработала метрика на клик в кнопку маршрута',
            );
        });
    });
});
