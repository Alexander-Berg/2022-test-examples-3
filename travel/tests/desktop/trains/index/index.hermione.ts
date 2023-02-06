import {assert} from 'chai';
import {index} from 'suites/trains';

import {SECOND} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

describe(index.name, () => {
    it('Работа популярных ж/д направлений', async function () {
        const app = new TestTrainsApp(this.browser);

        await app.goToIndexPage();

        const {indexPage} = app;

        assert.isTrue(
            await indexPage.crossLinksGallery.isVisible(),
            'Должны отображаться популярные направления',
        );

        const firstRecipe = await indexPage.crossLinksGallery.items.first();

        const from = await firstRecipe.getFrom();
        const to = await firstRecipe.getTo();

        await firstRecipe.click();

        await this.browser.switchToNextTab();

        const {directionPage} = app;

        await directionPage.title.waitForVisible(10 * SECOND);

        assert.equal(
            await directionPage.title.getText(),
            `Купить билеты на поезд ${from} — ${to}`,
        );
    });
});
