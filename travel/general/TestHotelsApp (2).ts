import {stringify} from 'querystring';
import {index, serp} from 'suites/hotels';

import {ITestHotelsSearchParams} from 'helpers/project/hotels/types/ITestHotelsSearchParams';

import {HotelsIndexPage} from 'helpers/project/hotels/pages/HotelsIndexPage/HotelsIndexPage';
import {TestHotelsSearchPage} from 'helpers/project/hotels/pages/HotelsSearchPage/TestHotelsSearchPage';
import {HotelsCityPage} from 'helpers/project/hotels/pages/HotelsCityPage/HotelsCityPage';
import {TestHotelPage} from 'helpers/project/hotels/pages/HotelPage/TestHotelPage';

export class TestHotelsApp {
    readonly indexPage: HotelsIndexPage;
    readonly searchPage: TestHotelsSearchPage;
    readonly cityPage: HotelsCityPage;
    readonly hotelPage: TestHotelPage;

    private readonly browser: WebdriverIO.Browser;

    constructor(browser: WebdriverIO.Browser) {
        this.browser = browser;

        this.indexPage = new HotelsIndexPage(browser);
        this.searchPage = new TestHotelsSearchPage(browser);
        this.cityPage = new HotelsCityPage(browser);
        this.hotelPage = new TestHotelPage(browser);
    }

    async goToIndexPage(): Promise<void> {
        await this.browser.url(index.url);
    }

    async goToSearchPage(searchParams: ITestHotelsSearchParams): Promise<void> {
        const {adults, geoId, childrenAges, checkinDate, checkoutDate} =
            searchParams;

        await this.browser.url(
            `${serp.url}?${stringify({
                geoId,
                adults,
                childrenAges: childrenAges ? childrenAges.join(',') : undefined,
                checkinDate,
                checkoutDate,
            })}`,
        );
    }
}
