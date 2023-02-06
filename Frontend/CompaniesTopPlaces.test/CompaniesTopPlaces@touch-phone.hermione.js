'use strict';

const PO = require('./CompaniesTopPlaces.page-object')('touch-phone');

const DEFAULT_ITEMS_COUNT = 7;
const ITEMS_LOAD_DEFAULT = 5;

const hideTiles = function(selector) {
    const style = document.createElement('style');

    style.innerText = `
    ${selector} .ymaps3x0--tile-layer { background: rgba(0, 0, 0, .1) !important }
    ${selector} .ymaps3x0--tile-layer__container { opacity: 0 !important }
    `;
    document.body.appendChild(style);
};

hermione.only.notIn('searchapp-phone');
specs('Список ресторанов Мишлен', function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'рестораны мишлен москва',
                data_filter: 'companies',
                srcparams: 'GEOV:experimental=add_snippet=business_awards_experimental/1.x',
            },
            PO.companiesTopPlaces(),
        );
    });

    hermione.also.in(['iphone-dark']);
    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.yaWaitForHiddenViaOpacity(PO.companiesTopPlaces.Map.placeholder(), 'Статическая карта не скрылась');
        await browser.yaWaitForVisible(PO.companiesTopPlaces.Map.pin(), 'Не загрузились пины на карте');
        await browser.execute(hideTiles, PO.companiesTopPlaces.Map());
        await browser.assertView('plain', PO.companiesTopPlaces());
    });

    it('Количество элементов в списке', async function() {
        const items = await this.browser.yaVisibleCount(PO.companiesTopPlaces.List.Item());

        assert.equal(items, DEFAULT_ITEMS_COUNT);
    });

    hermione.also.in(['iphone-dark']);
    it('Попап с информацией о Мишлен', async function() {
        const { browser } = this;

        await browser.yaCheckBaobabCounter(PO.companiesTopPlaces.help(), {
            path: '/$page/$main/$result/companies-top/help[@behaviour@type = "dynamic"]',
        });
        await browser.yaWaitForVisible(PO.michelinModal());
        await browser.pause(600);
        await browser.assertView('popup', PO.michelinModal(), {
            invisibleElements: 'body > *:not(.Drawer)',
        });
    });

    it('Открытие оверлея по клику в карточку', async function() {
        const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org"', {
            field: 'url',
        }, () => this.browser.click(PO.companiesTopPlaces.List.SecondItem.OverlayHandler()));

        assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

        await this.browser.yaWaitForVisible(PO.overlayOneOrg());
    });

    it('Проверка элемента после дозагрузки', async function() {
        const browser = this.browser;
        const items = await browser.yaVisibleCount(PO.companiesTopPlaces.List.Item());

        await browser.click(PO.companiesTopPlaces.List.more());

        // Ждём загрузку новых элементов
        await browser.yaWaitUntil('Новые элементы не загрузились', async function() {
            const newItems = await browser.yaVisibleCount(PO.companiesTopPlaces.List.Item());

            return (newItems - items) === ITEMS_LOAD_DEFAULT;
        });

        const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org"', {
            field: 'url',
        }, () => this.browser.yaCheckBaobabCounter(PO.companiesTopPlaces.List.Item11.OverlayHandler(), {
            path: '/$page/$main/$result/companies-top/map/minibadge/item',
            behaviour: {
                type: 'dynamic',
            },
        }));

        assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

        await browser.yaWaitForVisible(PO.overlayOneOrg());
    });

    it('Заголовок', async function() {
        const { browser } = this;

        await browser.yaCheckBaobabCounter(
            () => browser.click(PO.companiesTopPlaces.title.text()),
            {
                path: '/$page/$main/$result/companies-top/title',
                behaviour: { type: 'dynamic' },
            },
        );
        await browser.yaWaitForVisible(PO.overlayIframe(), 'Не открылся оверлей с iframe Карт');

        const src = await this.browser.getAttribute(PO.overlayIframe(), 'src');

        assert.isString(src, 'Нет атрибута src у iframe');

        const { origin, pathname, searchParams } = new URL(src);
        const errorMsg = param => `В ссылке iframe: ${src} сломан параметр "${param}"`;

        assert.equal(origin, 'https://yandex.ru', errorMsg('origin'));
        assert.equal(pathname, '/web-maps/', errorMsg('pathname'));
        assert.equal(searchParams.get('view-state'), 'micro', errorMsg('view-state'));
        assert.equal(searchParams.get('profile-mode'), '1', errorMsg('profile-mode'));
        assert.equal(searchParams.get('no-distribution'), '1', errorMsg('no-distribution'));
        assert.equal(searchParams.get('no-header'), '1', errorMsg('no-header'));
        assert.match(searchParams.get('ll'), /.+/, errorMsg('ll'));
        assert.match(searchParams.get('sll'), /.+/, errorMsg('sll'));
        assert.match(searchParams.get('sspn'), /.+/, errorMsg('sspn'));
        assert.match(searchParams.get('sspn'), /.+/, errorMsg('sspn'));
        assert.match(searchParams.get('sctx'), /.+/, errorMsg('sctx'));
        assert.match(searchParams.get('fixed_top'), /.+/, errorMsg('fixed_top'));
    });
});
