'use strict';

const PO = require('./PhotoRecipe.page-object/');

specs('Спецсниппет рецептов', () => {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'салат с рукколой и авокадо',
            foreverdata: '1549182457',
        }, PO.recipePhoto());
    });

    hermione.also.in('iphone-dark');
    it('Просмотр картинок', async function() {
        await this.browser.assertView('plain', PO.recipePhoto());

        await this.browser.yaCheckBaobabCounter(PO.recipePhoto.thumb(), {
            path: '/$page/$main/$result/thumb',
            behaviour: { type: 'dynamic' },
        }, 'Не сработал счётчик открытия просмотрщика');

        await this.browser.yaWaitForVisible(PO.imagesViewer2(), 'Просмотрщик картинок не появился');

        await this.browser.yaCheckBaobabCounter(async () => {
            await this.browser.click(PO.imagesViewer2.closeButton());
            return this.browser.yaWaitForHidden(PO.imagesViewer2(), 'Просмотрщик картинок не закрылся кликом по крестику');
        }, {
            path: '/$page/$main/$result/images-viewer/close',
            behaviour: { type: 'dynamic' },
        }, 'Не сработал счётчик закрытия просмотрщика');
    });
});
