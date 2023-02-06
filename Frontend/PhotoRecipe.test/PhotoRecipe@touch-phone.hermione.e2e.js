'use strict';

const PO = require('./PhotoRecipe.page-object/');

hermione.only.notIn('searchapp-phone');
specs('Спецсниппет рецептов', () => {
    it('Проверка показа сниппета с турбо', async function() {
        await this.browser.yaOpenSerp({
            text: 'плов повар рецепт',
        }, PO.recipePhoto());

        await this.browser.waitForVisible(PO.recipePhoto());
        await this.browser.waitForVisible(PO.recipePhoto.title());
        const url = await this.browser.getAttribute(PO.recipePhoto.title(), 'href');
        (await url.indexOf('//hamster.yandex.ru/turbo')) === 0;
    });
});
