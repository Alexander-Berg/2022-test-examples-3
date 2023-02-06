import moment from 'moment';
import {assert} from 'chai';
import {index} from 'suites/hotels';
import {URL, URLSearchParams} from 'url';

import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';

describe(index.name, () => {
    it('Отображается блок популярных мест', async function () {
        const app = new TestHotelsApp(this.browser);

        await app.goToIndexPage();

        assert.isTrue(await app.indexPage.crossLinksGallery.isDisplayed());
    });

    it('Есть хотя бы одно популярное место', async function () {
        const app = new TestHotelsApp(this.browser);

        await app.goToIndexPage();

        assert.isAbove(await app.indexPage.crossLinksGallery.items.count(), 0);
    });

    it('Заполнение и отправка формы поиска с переходом на поисковую отельную выдачу', async function () {
        const tomorrow = moment().add(1, 'days').format('YYYY-MM-DD');
        const afterTomorrow = moment().add(2, 'days').format('YYYY-MM-DD');
        const adults = 3;
        const childrenAges = [0, 3];
        const geo = {
            id: 1106,
            name: 'Грозный',
        };

        const app = new TestHotelsApp(this.browser);

        await app.goToIndexPage();

        const {searchForm} = app.indexPage;

        await searchForm.fill({
            adults,
            childrenAges,
            place: geo.name,
            checkinDate: tomorrow,
            checkoutDate: afterTomorrow,
        });

        await searchForm.submitButton.click();

        await this.browser.pause(1000);

        const currentUrl = await this.browser.getUrl();
        const {pathname, searchParams} = new URL(currentUrl);
        const query = parseQuery(searchParams);

        assert.match(
            pathname,
            /hotels\/search\//,
            'В адресной строке есть путь страницы отеля',
        );
        assert.equal(
            query.adults,
            adults,
            'Верное количество взрослых в параметрах адресной строки',
        );
        assert.equal(
            query.childrenAges,
            childrenAges.toString(),
            'Верное количество детей с возрастом в параметрах адресной строки',
        );
        assert.equal(
            query.checkinDate,
            tomorrow,
            'Верная дата заселения в параметрах адресной строки',
        );
        assert.equal(
            query.checkoutDate,
            afterTomorrow,
            'Верная дата выселения в параметрах адресной строки',
        );
        assert.equal(
            query.geoId,
            geo.id,
            'Верный идентификатор геолокации в параметрах адресной строки',
        );
    });

    it('Заполнение и отправка формы поиска с переходом на отель', async function () {
        const tomorrow = moment().add(1, 'days').format('YYYY-MM-DD');
        const afterTomorrow = moment().add(2, 'days').format('YYYY-MM-DD');
        const adults = 1;
        const hotel = {
            name: 'Грозный Сити Отель',
            urlRegex: /grozniy\/groznyi-siti/,
        };

        const app = new TestHotelsApp(this.browser);

        await app.goToIndexPage();

        const {searchForm} = app.indexPage;

        await searchForm.fill({
            adults,
            place: hotel.name,
            checkinDate: tomorrow,
            checkoutDate: afterTomorrow,
        });

        await searchForm.submitButton.click();

        await this.browser.pause(1000);

        const currentUrl = await this.browser.getUrl();
        const {pathname, searchParams} = new URL(currentUrl);
        const query = parseQuery(searchParams);

        assert.match(
            pathname,
            hotel.urlRegex,
            'В адресной строке есть путь страницы отеля',
        );
        assert.equal(
            query.adults,
            adults,
            'Верное количество взрослых в параметрах адресной строки',
        );
        assert.equal(
            query.checkinDate,
            tomorrow,
            'Верная дата заселения в параметрах адресной строки',
        );
        assert.equal(
            query.checkoutDate,
            afterTomorrow,
            'Верная дата выселения в параметрах адресной строки',
        );
    });
});

function parseQuery(searchParams: URLSearchParams): {
    adults: number;
    checkinDate: string | null;
    checkoutDate: string | null;
    childrenAges: string | null;
    geoId: number;
    hotelPermalink: number;
} {
    const adults = Number(searchParams.get('adults'));
    const childrenAges = searchParams.get('childrenAges');
    const checkinDate = searchParams.get('checkinDate');
    const checkoutDate = searchParams.get('checkoutDate');
    const geoId = Number(searchParams.get('geoId'));
    const hotelPermalink = Number(searchParams.get('hotelPermalink'));

    return {
        adults,
        checkinDate,
        checkoutDate,
        childrenAges,
        geoId,
        hotelPermalink,
    };
}
