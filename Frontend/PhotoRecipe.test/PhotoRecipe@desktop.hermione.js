'use strict';

const PO = require('./PhotoRecipe.page-object');

specs({
    feature: 'Спецсниппет рецептов',
}, function() {
    it('Обязательные проверки', async function() {
        await this.browser.yaOpenSerp({
            data_filter: { values: ['photo-recipe'], operation: 'AND' },
            foreverdata: 1549182457,
        }, PO.recipePhoto());

        await this.browser.assertView('plain', PO.recipePhoto());
        await this.browser.yaCheckLink2({ selector: PO.recipePhoto.title() });
        await this.browser.yaCheckLink2({ selector: PO.recipePhoto.greenUrl() });
        await this.browser.yaCheckLink2({ selector: PO.recipePhoto.thumb() });
    });
});
