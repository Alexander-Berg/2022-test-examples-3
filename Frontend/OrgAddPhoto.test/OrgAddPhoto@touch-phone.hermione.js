'use strict';

const PO = require('./OrgAddPhoto.page-object').touchPhone;

hermione.also.in('iphone-dark');
specs({
    feature: 'Одна организация',
    type: 'Кнопка добавления фото',
}, function() {
    async function openSerp(browser, params = {}) {
        await browser
            .yaOpenSerp({
                text: 'кафе пушкин',
                data_filter: 'companies',
                ...params,
            }, PO.oneOrg());
    }

    it('При отсутствии фотографий', async function() {
        const { browser } = this;

        await openSerp(browser, { text: 'Территориальный участок 6202 по Захаровскому району Межрайонной ИФНС России № 5 по Рязанской области' });
        await browser.yaShouldBeVisible(PO.oneOrg.OrgAddPhoto(), 'Нет блока с кнопкой');
        await browser.assertView('plain', PO.oneOrg.OrgAddPhoto());
    });
});
