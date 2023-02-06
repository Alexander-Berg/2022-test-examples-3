// @flow

// flowlint-next-line untyped-import:off
import {screen} from '@testing-library/dom';

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {resolveSearchByInitialParamsMock} from './__mocks__';

const FilterName = {
    ALL: /любой/i,
    ONE_TWO_HOURS: /1-2 часа/i,
    TODAY: /^сегодня$/i,
    TODAY_TOMORROW: /сегодня или завтра/i,
    FIVE_DAYS: /до 5 дней/i,
};

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext({params = {}} = {}) {
    return mandrelLayer.initContext({
        request: {
            cookie: {
                kadavr_session_id: await kadavrLayer.getSessionId(),
            },
            params: {
                slug: 'telefony',
                nid: 12345678,
                hid: 91491,
                ...params,
            },
        },
    });
}
// MARKETFRONT-96354
// Widget: SearchFilters Фильтр "Срок доставки" на смешанной выдаче Список значенией отображается корректно
// Падений 8.26%
// Widget: SearchFilters Фильтр "Срок доставки" на экспрессной выдаче Список значенией отображается корректно
// Падений 8.26%
// eslint-disable-next-line jest/no-disabled-tests
describe.skip('Widget: SearchFilters', () => {
    const WIDGET_PATH = '@self/root/src/widgets/content/search/Filters';
    const WIDGET_OPTIONS = {
        wrapperProps: {font: {size: '200'}, margins: {bottom: '5'}},
        props: {scrollToAnchor: 'serpTop', searchPlace: '__standalone__'},
    };

    beforeAll(async () => {
        mockIntersectionObserver();

        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });

        jestLayer = mirror.getLayer('jest');
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');

        await jestLayer.backend.runCode(resolveSearchByInitialParamsResult => {
            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/root/src/resolvers/search'), 'resolveSearchSizeTableDeclaration')
                .mockReturnValue(Promise.resolve(undefined));

            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/root/src/resolvers/cms'), 'resolveFilterImages')
                .mockReturnValue(Promise.resolve({collections: {filterImage: {}}, result: []}));

            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/root/src/resolvers/search/resolveFiltersDescription'), 'resolveFiltersDescription')
                .mockReturnValue(Promise.resolve({
                    collections: {filterDescription: {'delivery-interval': {id: 'delivery-interval'}}},
                    result: ['delivery-interval'],
                }));

            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/root/src/resolvers/search/resolveSearchView/resolveSearchModeFromCmsConfig'), 'resolveSearchModeFromCmsConfig')
                .mockReturnValue(Promise.resolve(undefined));

            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/root/src/resolvers/search/resolveSearchByInitialParams'), 'default')
                .mockReturnValue(Promise.resolve(resolveSearchByInitialParamsResult));
        }, [resolveSearchByInitialParamsMock]);
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Фильтр "Срок доставки" на смешанной выдаче', () => {
        test('Список значенией отображается корректно', async () => {
            await makeContext();
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);

            const allIntervalsButton = await screen.findByRole('radio', {name: FilterName.ALL});
            expect(allIntervalsButton).toBeVisible();
            expect(allIntervalsButton).toBeChecked();

            const oneTwoHoursIntervalButton = await screen.findByRole('radio', {name: FilterName.ONE_TWO_HOURS});
            expect(oneTwoHoursIntervalButton).toBeVisible();

            const oneTwoHoursIntervalIcon = await screen.findByRole('img', {hidden: true});
            expect(oneTwoHoursIntervalIcon).toBeVisible();

            const todayIntervalButton = await screen.findByRole('radio', {name: FilterName.TODAY});
            expect(todayIntervalButton).toBeVisible();

            const todayTomorrowIntervalButton = await screen.findByRole('radio', {name: FilterName.TODAY_TOMORROW});
            expect(todayTomorrowIntervalButton).toBeVisible();

            const fiveDaysIntervalButton = await screen.findByRole('radio', {name: FilterName.FIVE_DAYS});
            expect(fiveDaysIntervalButton).toBeVisible();
        });
    });

    describe('Фильтр "Срок доставки" на экспрессной выдаче', () => {
        test('Список значенией отображается корректно', async () => {
            await makeContext({params: {searchContext: 'express'}});
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);

            const allIntervalsButton = await screen.findByRole('radio', {name: FilterName.ALL});
            expect(allIntervalsButton).toBeVisible();
            expect(allIntervalsButton).toBeChecked();

            const oneTwoHoursIntervalButton = await screen.findByRole('radio', {name: FilterName.ONE_TWO_HOURS});
            expect(oneTwoHoursIntervalButton).toBeVisible();

            const oneTwoHoursIntervalIcon = container.querySelector('img');
            expect(oneTwoHoursIntervalIcon).toBe(null);

            const todayIntervalButton = await screen.findByRole('radio', {name: FilterName.TODAY});
            expect(todayIntervalButton).toBeVisible();

            const todayTomorrowIntervalButton = await screen.findByRole('radio', {name: FilterName.TODAY_TOMORROW});
            expect(todayTomorrowIntervalButton).toBeVisible();

            const fiveDaysIntervalButton = await screen.findByRole('radio', {name: FilterName.FIVE_DAYS});
            expect(fiveDaysIntervalButton).toBeVisible();
        });
    });
});
