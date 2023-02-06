import {assert} from 'chai';
import {index} from 'suites/trains';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

describe(index.name, () => {
    it('Отображается блок популярных направлений', async function () {
        const app = new TestTrainsApp(this.browser);

        await app.goToIndexPage();

        const isDisplayed = await app.indexPage.crossLinksGallery.isDisplayed();

        assert.isTrue(isDisplayed);
    });
});
