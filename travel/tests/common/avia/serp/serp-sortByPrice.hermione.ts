import {serp} from 'suites/avia';
import moment from 'moment';
import {assert} from 'chai';

import dateFormats from 'helpers/utilities/date/formats';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {ESortingType} from 'helpers/project/avia/pages/SearchResultsPage/components/Sorting';

describe(serp.name, () => {
    it('Проверка сортировки по стоимости', async function () {
        const testApp = new TestAviaApp(this.browser);

        await testApp.goToSearchPage({
            from: {name: 'Екатеринбург', id: 'c54'},
            to: {name: 'Санкт-Петербург', id: 'c2'},
            startDate: moment().add(1, 'months').format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
        });

        const desktopSearchPage = testApp.searchDesktopPage;
        const touchSearchPage = testApp.searchPage;

        async function getPriceValues(): Promise<number[]> {
            return desktopSearchPage.variants.map(async prices => {
                return prices.price.getPriceValue();
            });
        }

        await desktopSearchPage.waitForSearchComplete();

        const sorting = desktopSearchPage.isDesktop
            ? desktopSearchPage.sorting
            : touchSearchPage.sorting;

        assert.isTrue(
            await sorting.order.isVisible(),
            'Должна отображаться кнопка порядка сортировки',
        );

        assert.isTrue(
            await sorting.isAscOrder(),
            'Порядок сортировки должен быть по возрастанию',
        );

        await sorting.selectSortOption(ESortingType.INTEREST);
        assert.equal(
            await sorting.typeSelect.getValue(),
            'Сначала рекомендуемые',
            'На кнопке должно быть указано "Сначала рекомендуемые"',
        );

        await sorting.selectSortOption(ESortingType.DEPARTURE);
        assert.equal(
            await sorting.typeSelect.getValue(),
            'По времени отправления',
            'На кнопке должно быть указано "По времени отправления"',
        );

        await sorting.selectSortOption(ESortingType.ARRIVAL);
        assert.equal(
            await sorting.typeSelect.getValue(),
            'По времени прибытия',
            'На кнопке должно быть указано "По времени прибытия"',
        );

        await sorting.selectSortOption(ESortingType.DURATION);
        assert.equal(
            await sorting.typeSelect.getValue(),
            'По времени в пути',
            'На кнопке должно быть указано "По времени в пути"',
        );

        await sorting.selectSortOption(ESortingType.PRICE);
        assert.equal(
            await sorting.typeSelect.getValue(),
            'По цене',
            'На кнопке должно быть указано "По цене"',
        );

        const resultAsc = await getPriceValues();

        const checkAscSort = (b: number[]): boolean =>
            b.every((v, i, a) => !i || a[i - 1] <= v);

        assert.isTrue(
            checkAscSort(resultAsc),
            'Значения не отсортированы по возрастанию',
        );

        await desktopSearchPage.sorting.order.clickJS();

        const resultDesc = await getPriceValues();

        const checkDescSort = (b: number[]): boolean =>
            b.every((v, i, a) => !i || a[i - 1] >= v);

        assert.isTrue(
            checkDescSort(resultDesc),
            'Значения не отсортированы по убыванию',
        );
    });
});
