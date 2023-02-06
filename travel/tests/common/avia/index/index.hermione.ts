import moment from 'moment';
import {assert} from 'chai';

import {urlAssertor} from 'helpers/project/common/urlAssertor';
import TestApp from 'helpers/project/TestApp';

const {
    index: {name: suiteName, url},
} = require('suites/avia');

const MOSCOW_GEO = {
    name: 'Москва',
    geoId: '213',
    title: 'Вылет из Москвы',
};
const EKATERINBURG_GEO = {
    name: 'Екатеринбург',
    geoId: '54',
    title: 'Вылет из Екатеринбурга',
};

describe(suiteName, () => {
    it('Заполнение и отправка формы поиска авиабилетов с переходом на поисковую выдачу', async function () {
        const app = new TestApp(this.browser);
        const {aviaApp} = app;

        await aviaApp.goToIndexPage();

        const {indexPage} = aviaApp;

        // ACT
        const startDate = moment().add(1, 'days').format('YYYY-MM-DD');
        const endDate = moment().add(8, 'days').format('YYYY-MM-DD');

        const count = 1;
        const expectedKlass = 'business';

        const {searchForm} = indexPage;

        await searchForm.fill({
            fromName: EKATERINBURG_GEO.name,
            toName: MOSCOW_GEO.name,
            when: startDate,
            return_date: endDate,
            adult_seats: count,
            children_seats: count,
            infant_seats: count,
            klass: expectedKlass,
        });

        await searchForm.submitButton.click();

        // ASSERT
        await urlAssertor(this.browser, ({searchParams}) => {
            const fromId = searchParams.get('fromId');
            const toId = searchParams.get('toId');

            const adults = searchParams.get('adult_seats');
            const children = searchParams.get('children_seats');
            const infant = searchParams.get('infant_seats');
            const klass = searchParams.get('klass');

            const when = searchParams.get('when');
            const returnDate = searchParams.get('return_date');

            assert.equal(fromId, `c${EKATERINBURG_GEO.geoId}`);
            assert.equal(toId, `c${MOSCOW_GEO.geoId}`);

            assert.equal(Number(adults), count);
            assert.equal(Number(children), count);
            assert.equal(Number(infant), count);
            assert.equal(klass, expectedKlass);

            assert.equal(when, startDate);
            assert.equal(returnDate, endDate);
        });
    });

    it('По умолчанию выбрана вкладка авиабилетов', async function () {
        const app = new TestApp(this.browser);
        const {aviaApp} = app;

        await aviaApp.goToIndexPage();

        const {indexPage} = aviaApp;

        const {pathname, text} = await indexPage.indexTabs.getActiveTabInfo();

        assert.include(pathname, url, 'Таб не содержит ожидаемый путь сервиса');
        assert.match(text, /^авиа$/i, 'Таб не содержит ожидаемое имя сервиса');
    });

    it('Поиск рейсов без выбора количества пассажиров', async function () {
        const app = new TestApp(this.browser);
        const {aviaApp} = app;

        await aviaApp.goToIndexPage();

        const {indexPage} = aviaApp;

        const {searchForm} = indexPage;

        const startDate = moment().add(1, 'days').format('YYYY-MM-DD');

        await searchForm.fill({
            fromName: EKATERINBURG_GEO.name,
            toName: MOSCOW_GEO.name,
            when: startDate,
        });

        await searchForm.submitButton.click();

        await urlAssertor(this.browser, ({pathname, searchParams}) => {
            assert.include(
                pathname,
                'avia/search/result',
                'Не произошел переход на поиск',
            );
            assert.equal(
                Number(searchParams.get('adult_seats')),
                1,
                'Неожиданное количество взрослых мест',
            );
            assert.equal(
                Number(searchParams.get('children_seats')),
                0,
                'Неожиданное количество детских мест',
            );
            assert.equal(
                Number(searchParams.get('infant_seats')),
                0,
                'Неожиданное количество младенческих мест',
            );
        });
    });

    it('Поиск рейсов c выбором количества пассажиров', async function () {
        const app = new TestApp(this.browser);
        const {aviaApp} = app;

        await aviaApp.goToIndexPage();

        const {indexPage} = aviaApp;

        const {searchForm} = indexPage;

        const startDate = moment().add(1, 'days').format('YYYY-MM-DD');

        await searchForm.fill({
            fromName: EKATERINBURG_GEO.name,
            toName: MOSCOW_GEO.name,
            when: startDate,
            adult_seats: 3,
            children_seats: 1,
            infant_seats: 2,
        });

        await searchForm.submitButton.click();

        await urlAssertor(this.browser, ({pathname, searchParams}) => {
            assert.include(
                pathname,
                'avia/search/result',
                'Не произошел переход на поиск',
            );
            assert.equal(
                Number(searchParams.get('adult_seats')),
                3,
                'Неожиданное количество взрослых мест',
            );
            assert.equal(
                Number(searchParams.get('children_seats')),
                1,
                'Неожиданное количество детских мест',
            );
            assert.equal(
                Number(searchParams.get('infant_seats')),
                2,
                'Неожиданное количество младенческих мест',
            );
        });
    });
});
