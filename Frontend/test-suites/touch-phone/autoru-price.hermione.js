'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;
const params = {
    skipProtocol: true,
    skipQuery: true
};

specs({
    feature: 'Колдунщик Авто.ру',
    type: 'Витринный'
}, function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp({ foreverdata: 4178499792 }, PO.autoPrice())
            .yaWaitForVisible(PO.autoPrice())
            .yaHideElement(PO.smartBanner());
    });

    it('Заголовок', function() {
        return this.browser
            .assertView('plain', PO.autoPrice(), {
                ignoreElements: PO.autoPrice.showcase.item.thumb()
            })
            .yaCheckBaobabCounter(PO.autoPrice.title.link(), {
                path: '/$page/$main/$result[@wizard_name="autoru/thumbs-price"]/title'
            })
            .yaCheckLink(PO.autoPrice.title.link(), { target: '_blank' })
            .then(url => this.browser
                .yaCheckURL(url, 'https://m.auto.ru/moskva/trucks/all/', params)
            );
    });

    it('Гринурл', function() {
        return this.browser
            .yaCheckBaobabCounter(PO.autoPrice.path.firstLink(), {
                path: '/$page/$main/$result[@wizard_name="autoru/thumbs-price"]/path/urlnav'
            })
            .yaCheckLink(PO.autoPrice.path.firstLink(), { target: '_blank' })
            .then(url => this.browser
                .yaCheckURL(url, 'https://m.auto.ru/moskva/trucks/all/', params)
            );
    });

    it('Фотография элемента витрины', async function() {
        return this.browser
            .yaHideElement(PO.smartBanner())
            .yaCheckBaobabCounter(PO.autoPrice.firstItemThumbLink(), {
                path: '/$page/$main/$result[@wizard_name="autoru/thumbs-price"]/showcase/thumb'
            })
            .yaCheckLink(PO.autoPrice.firstItemThumbLink(), { target: '_blank' })
            .then(url => this.browser
                .yaCheckURL(url, 'https://m.auto.ru/moskva/trucks/all/', {
                    ...params,
                    onlyExistence: ['pinned_offer_id']
                })
            );
    });

    it('Подпись элемента витрины', function() {
        return this.browser
            .yaCheckBaobabCounter(PO.autoPrice.firstItemSubtitle(), {
                path: '/$page/$main/$result[@wizard_name="autoru/thumbs-price"]/showcase/subtitle'
            })
            .yaCheckLink(PO.autoPrice.firstItemSubtitle(), { target: '_blank' })
            .then(url => this.browser
                .yaCheckURL(url, 'https://m.auto.ru/moskva/trucks/all/', {
                    ...params,
                    onlyExistence: ['pinned_offer_id']
                })
            );
    });
});
