'use strict';

const PO = require('./CompaniesMapGallery.page-object').touch;

specs({
    feature: 'Одна организация',
    type: 'Галерея с фото и картой',
}, function() {
    describe('Общие проверки', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'кафе пушкин',
                srcskip: 'YABS_DISTR',
                data_filter: 'companies',
            }, PO.oneOrg.companiesMapGallery());
        });

        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.yaStubImage(`${PO.oneOrg.companiesMapGallery.map()} img`, 610, 100);
            await browser.assertView('plain', PO.oneOrg.companiesMapGallery());
        });

        it('Клик в карту', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(
                PO.oneOrg.companiesMapGallery.map(),
                { path: '/$page/$main/$result/composite/companies-map-gallery/photo-tiles/scroller/map[@behaviour@type="dynamic"]' },
            );
            await browser.yaWaitForVisible(PO.overlayIframe(), 'Не открылся оверлей по клику в карту');
            await checkOneOrgIframe.call(this, PO.overlayIframe());
        });

        it('Увеличение после скрола', async function() {
            const { browser } = this;

            await browser.yaScrollContainer(PO.oneOrg.companiesMapGallery.photoTiles.scrollerWrap(), 100);
            await browser.yaScrollContainer(PO.oneOrg.companiesMapGallery.photoTiles.scrollerWrap(), 0);

            await browser.yaWaitForVisible(PO.oneOrg.companiesMapGalleryExpanded());

            await browser.yaStubImage(`${PO.oneOrg.companiesMapGallery.map()} img`, 610, 200);
            await browser.assertView('expanded', PO.oneOrg.companiesMapGalleryExpanded());
        });

        it('Все фото', async function() {
            const { browser } = this;

            await browser.yaScroll(PO.oneOrg.companiesMapGallery.photoTiles.secondItem());
            await browser.yaCheckBaobabCounter(PO.oneOrg.companiesMapGallery.photoTiles.secondItem(), {
                path: '/$page/$main/$result/composite/companies-map-gallery/photo-tiles/scroller/item[@type = "All"]',
                behaviour: { type: 'dynamic' },
            });
            await browser.yaWaitForVisible(PO.imagesViewer2(), 'Просмотрщик не открылся');
        });

        it('Кнопка "Все фото"', async function() {
            const { browser } = this;

            await browser.yaScrollContainer(PO.oneOrg.companiesMapGallery.photoTiles.scrollerWrap(), 9999);
            await browser.yaShouldBeVisible(PO.oneOrg.companiesMapGallery.photoTiles.more(), 'Нет кнопки Ещё');

            const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org-photo"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(
                () => browser.click(PO.oneOrg.companiesMapGallery.photoTiles.more()), {
                    path: '/$page/$main/$result/composite/companies-map-gallery/photo-tiles/scroller/more',
                    behaviour: { type: 'dynamic' },
                }));

            assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

            await browser.yaWaitForVisible(PO.overlayOneOrg.photoStream(), 'Не открылся оверлей на табе Фото');
            await browser.yaWaitForVisible(PO.overlayOneOrg.photoStream.tags.checkedItem(), 'Нет активного тега');
            await browser.yaShouldBeSame(
                PO.overlayOneOrg.photoStream.tags.checkedItem(),
                PO.overlayOneOrg.photoStream.tags.firstItem(),
                'Активирован не первый тег табе Фото',
            );
            await browser.yaWaitForVisible(PO.overlayOneOrg.photoStream.tiles.firstItem(),
                'Содержимое таба Фото не загрузилось');
        });
    });

    it('Карта в отелях открывает серповый сайдблок', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'отель рэдиссон славянская',
            srcparams: 'GEOV:experimental=add_snippet=photos/3.x',
            data_filter: 'companies',
        }, PO.oneOrg.companiesMapGallery());

        await browser.yaCheckBaobabCounter(
            PO.oneOrg.companiesMapGallery.map(),
            { path: '/$page/$main/$result/composite/companies-map-gallery/photo-tiles/scroller/map' },
        );
        await browser.yaWaitForVisible(PO.overlayOneOrgMap(), 'Не открылся оверлей по клику в карту');
    });

    it('Проверка externalId', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            srcskip: 'YABS_DISTR',
            data_filter: 'companies',
        }, PO.oneOrg.companiesMapGallery());

        await this.browser.yaCheckBaobabCounter(PO.oneOrg.companiesMapGallery.map(), {
            path: '/$page/$main/$result[@externalId@entity="organization" and @externalId@id="1018907821"]/composite/companies-map-gallery/photo-tiles/scroller/map',
        });
    });
});

/** Проверяет ссылку для iframe Карт одной организации */
async function checkOneOrgIframe(selector) {
    const src = await this.browser.getAttribute(selector, 'src');

    assert.isString(src, 'Нет атрибута src у iframe');

    const { origin, pathname } = new URL(src);
    const errorMsg = param => `В ссылке iframe: ${src} сломан параметр "${param}"`;

    assert.equal(origin, 'https://yandex.ru', errorMsg('origin'));
    assert.match(pathname, /^\/profile\/\d+$/, errorMsg('pathname'));
}
