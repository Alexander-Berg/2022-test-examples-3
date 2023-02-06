import {assert} from 'chai';
import {index, serp} from 'suites/avia';
import moment from 'moment';

import {SECOND} from 'helpers/constants/dates';

import dateFormats from 'helpers/utilities/date/formats';
import {
    TestIndexAviaPage,
    AviaSearchResultsDesktopPage,
    AviaSearchResultsPage,
} from 'helpers/project/avia/pages';

describe(serp.name, () => {
    it('Як (пустой поиск)', async function () {
        await this.browser.url(index.url);

        const indexPage = new TestIndexAviaPage(this.browser);

        await indexPage.search({
            fromName: 'Ухта',
            toName: 'Капо-Ватикано',
            when: moment().add(1, 'month').format(dateFormats.ROBOT),
        });

        const search = indexPage.isTouch
            ? new AviaSearchResultsPage(this.browser)
            : new AviaSearchResultsDesktopPage(this.browser);

        await search.noResultsYak.title.waitForVisible(10 * SECOND);

        assert.equal(
            await search.noResultsYak.title.getText(),
            'Нет вариантов',
            'Должен быть текст "Нет вариантов"',
        );
        assert.equal(
            await search.noResultsYak.text.getText(),
            'Мы не нашли билетов на это направление.\nДавайте поищем куда-нибудь еще!',
            'Должен быть текст "Мы не нашли билетов на это направление. Давайте поищем куда-нибудь еще!"',
        );
    });
});
