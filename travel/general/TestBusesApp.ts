import moment, {Moment} from 'moment';
import {BASE_URL, IBusesSuitSearchPoint, SUITES} from 'suites/buses';

import dateFormats from 'helpers/utilities/date/formats';
import TestBusesIndexPage from 'helpers/project/buses/pages/TestBusesIndexPage/TestBusesIndexPage';
import TestBusesSearchDatePage from 'helpers/project/buses/pages/TestBusesSearchDatePage/TestBusesSearchDatePage';

export class TestBusesApp {
    indexPage: TestBusesIndexPage;
    searchPage: TestBusesSearchDatePage;

    protected readonly browser: WebdriverIO.Browser;

    constructor(browser: WebdriverIO.Browser) {
        this.browser = browser;

        this.indexPage = new TestBusesIndexPage(browser);
        this.searchPage = new TestBusesSearchDatePage(browser);
    }

    async goToIndexPage(): Promise<void> {
        await this.browser.url(BASE_URL);

        await this.indexPage.adfoxBanner.disableEvents();
    }

    async goToSearchPage(
        fromSlug: string,
        toSlug: string,
        when: Moment,
    ): Promise<void> {
        await this.browser.url(
            SUITES.pages.search.date.getUrl({
                fromSlug,
                toSlug,
                when: when.format(dateFormats.ROBOT),
            }),
        );

        await this.searchPage.waitUntilLoaded();
    }

    /**
     * Находит и переходит на одну из не пустых страниц поиска
     * где есть минимум два сегмента
     */
    async goToFilledSearchPage(): Promise<{
        route: IBusesSuitSearchPoint[];
        when: Moment;
    }> {
        const {routes} = SUITES;
        const now = moment();

        for (const route of routes) {
            const [from, to] = route;

            for (let i = 0; i < 3; i++) {
                const when = now.clone().add(i, 'days');

                await this.goToSearchPage(from.slug, to.slug, when);

                if (await this.searchPage.isEmptySearchPage()) {
                    continue;
                }

                if ((await this.searchPage.segments.items.items).length < 2) {
                    continue;
                }

                return {
                    route,
                    when,
                };
            }
        }

        throw new Error(
            'Не найдена страница поиска с двумя и более сегментами',
        );
    }
}
