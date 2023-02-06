'use strict';

const PO = require('./HotelsCarouselList.page-object');

specs({
    feature: 'Hotels / Карусель',
    experiment: 'Редизайн карусели, список отелей',
}, function() {
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'отели в алуште',
            srcrwr: 'GEOV:::1000000000',
            data_filter: 'companies',
        }, PO.companiesMapList());

        await this.browser.yaWaitForVisible(PO.companiesMapList.HotelsCarouselList(), 'Не появился список отелей');

        // уводим курсор перед снятием скрина
        await this.browser.moveToObject('body', 0, 0);

        // скрываем карту, чтобы тест был стабильнее
        // не используем hideElements потому что он скрывает кнопку "развернуть карту"
        await this.browser.execute(function(selector) {
            const mapImage = document.querySelector(selector);

            if (mapImage) {
                mapImage.style.opacity = 0;
            }
        }, PO.companiesMapList.HotelsCarouselList.MapCard.image());

        await this.browser.assertView('list', PO.companiesMapList.HotelsCarouselList());
    });

    it('Дозагрузка карточек по доскролу', async function() {
        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'отели в алуште',
            srcrwr: 'GEOV:::1000000000',
            data_filter: 'companies',
        }, PO.companiesMapList());

        await this.browser.yaWaitForVisible(PO.companiesMapList.HotelsCarouselList(), 'Не появился список отелей');

        const cardsCount = await this.browser.execute(function(selector) {
            return document.querySelectorAll(selector).length;
        }, PO.companiesMapList.HotelsCarouselList.HotelCard());

        await this.browser.yaScroll(PO.companiesMapList.HotelsCarouselList.LastHotelCard());
        await this.browser.yaWaitForHidden(PO.companiesMapList.HotelsCarouselList.Spin());

        const newCardsCount = await this.browser.execute(function(selector) {
            return document.querySelectorAll(selector).length;
        }, PO.companiesMapList.HotelsCarouselList.HotelCard());

        assert.isTrue(newCardsCount > cardsCount, 'После доскролла до конца карусели не догрузились новые карточки');

        await this.browser.yaScroll(PO.companiesMapList.HotelsCarouselList.LastHotelCard());
        await this.browser.yaWaitForHidden(PO.companiesMapList.HotelsCarouselList.Spin());

        const oids = await this.browser.execute(function(selector) {
            const links = document.querySelectorAll(selector);
            return [].map.call(links, link => {
                const url = new URL(link.getAttribute('href'));
                return url.searchParams.get('oid');
            });
        }, PO.companiesMapList.HotelsCarouselList.HotelCard.TopicLink());

        assert.equal(oids.length, (new Set(oids)).size, 'Отели не должны повторяться');
    });

    it('Открытие попапа при клике в карточку', async function() {
        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'отели в алуште',
            srcrwr: 'GEOV:::1000000000',
            data_filter: 'companies',
            rearr: 'scheme_Local/OrgWizard/ForceCarousel=1',
        }, PO.companiesMapList());

        await this.browser.yaWaitForVisible(PO.companiesMapList.HotelsCarouselList(), 'Не появился список отелей');

        await this.browser.yaCheckLink2({
            selector: PO.companiesMapList.HotelsCarouselList.HotelCard.TopicLink(),
            url: {
                href: '/search',
                queryValidator: query => {
                    assert(query.text, 'нет параметра text в ссылке карточки');
                    assert(query.oid, 'нет параметра oid в ссылке карточки');
                    assert(query.ag_dynamic, 'нет параметра ag_dynamic в ссылке карточки');

                    return true;
                },
                ignore: ['protocol', 'hostname'],
            },
            baobab: {
                path: '/$page/$top/$result/companies-map-list/hotels-carousel-list/hotel-list/item/link[@behaviour@type="dynamic"]',
            },
            // кликаем в тумбу (по дефолту гермиона кликает в середину карточки — клик попадает в тайтл)
            clickCoords: [-30, -30],
            message: 'Ссылка с карточки отеля должна вести на серп с поиском этого отеля',
        });
        await this.browser.yaWaitForVisible(PO.modal.oneOrg(), 'Не открылось окно c подробным описанием отеля');
    });

    it('Применение фильтра услуг вызывает отправку запроса с данными о фильтре', async function() {
        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'отели в алуште',
            srcrwr: 'GEOV:::1000000000',
            data_filter: 'companies',
            rearr: 'scheme_Local/OrgWizard/ForceCarousel=1',
        }, PO.companiesMapList());

        const form = PO.hotelsCarouselForm;
        const value = await this.browser.yaGetLastAjaxDataFast('filter=hotels_with_breakfast', {
            field: 'url',
            encodeDisable: true,
            message: 'Запрос с указанным фильтром не найден',
        }, async () => {
            await this.browser.yaCheckBaobabCounter(
                () => {
                    this.browser.click(form.servicesSelect());
                    this.browser.click(form.servicesPopup.secondOption());
                },
                { path: '/$page/$top/$result/companies-map-list/hotels-form-carousel/filters' },
            );
        });

        assert.isNotNull(value, 'Запрос с фильтром не отправился');
    });
});
