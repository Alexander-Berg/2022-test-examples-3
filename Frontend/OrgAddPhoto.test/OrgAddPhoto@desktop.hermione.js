'use strict';

const PO = require('./OrgAddPhoto.page-object').desktop;

specs({
    feature: 'Одна организация',
    type: 'Кнопка добавления фото',
}, function() {
    async function openSerpWithAddPhotoButton(browser, params = {}) {
        await browser
            .yaOpenSerp({
                text: 'кафе пушкин',
                data_filter: 'companies',
                ...params,
            }, PO.oneOrg());
        await browser.yaShouldBeVisible(PO.oneOrg.OrgAddPhoto(), 'Нет блока с кнопкой');
    }

    it('При отсутствии фотографий', async function() {
        const { browser } = this;

        await openSerpWithAddPhotoButton(browser, { text: 'Пещера Графский грот' });
        await browser.assertView('plain', PO.oneOrg.OrgAddPhoto());
        await browser.yaCheckBaobabCounter(PO.oneOrg.OrgAddPhoto(),
            {
                path: '/$page/$parallel/$result/composite/tabs/about/add-photo[@view="card"]/button',
            },
        );
        await browser.yaWaitForVisible(PO.addPhotoModal());
    });

    it('Для орги с 1 фотографией', async function() {
        const { browser } = this;

        await openSerpWithAddPhotoButton(browser, { text: 'автоинотех зарайск московская улица 18' });
        await browser.assertView('plain', PO.oneOrg.OrgAddPhoto());
        await browser.yaCheckBaobabCounter(() => browser.yaClickAtTheMiddle(PO.oneOrg.OrgAddPhoto()),
            {
                path: '/$page/$parallel/$result/composite/tabs/about/add-photo[@view="badge"]/button',
            },
        );
        await browser.yaWaitForVisible(PO.addPhotoModal());
    });
});
