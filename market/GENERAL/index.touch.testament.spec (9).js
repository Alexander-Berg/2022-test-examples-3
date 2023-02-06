// @flow

import {screen} from '@testing-library/dom';
import SharedActionEmitter from '@yandex-market/apiary/client/sharedActionEmitter';

import {makeMirrorTouch as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds/index.market.touch';
import {OPEN_FILTER} from '@self/platform/actions/filter';

import {widgetOptionsMock} from './__mocks__';

const FilterName = {
    ALL: /все/i,
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
                oFilSw: 1,
            },
            params: {
                slug: 'telefony',
                nid: 12345678,
                ...params,
            },
        },
        route: {
            name: PAGE_IDS_COMMON.LIST_FILTERS,
        },
    });
}

describe('Widget: SearchFilters', () => {
    const WIDGET_PATH = '@self/platform/widgets/content/SearchFilters';
    const WIDGET_OPTIONS = widgetOptionsMock;

    beforeAll(async () => {
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

        await jestLayer.backend.runCode(() => {
            jest.spyOn(require('@self/project/src/resolvers/spvConfigs/resolveVendorsConfig'), 'default')
                .mockReturnValue(Promise.resolve({}));

            jest.spyOn(require('@self/root/src/resolvers/cms/resolveContextPage'), 'resolveContextPage')
                .mockReturnValue(Promise.resolve({
                    result: [],
                    collections: {
                        cmsDeclaration: {},
                        cmsEntrypoint: {},
                    },
                }));
        }, []);
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Фильтр "Срок доставки" на смешанной выдаче', () => {
        test('Список значенией отображается корректно', async () => {
            await makeContext();
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);

            const globalSharedActionEmitter = new SharedActionEmitter();
            globalSharedActionEmitter.dispatch({
                type: OPEN_FILTER,
                payload: {filterId: 'delivery-interval'},
            });

            const allIntervalsButton = await screen.findByRole('radio', {name: FilterName.ALL});
            expect(allIntervalsButton).toBeVisible();
            expect(allIntervalsButton).toBeChecked();

            const oneTwoHoursIntervalButton = await screen.findByRole('radio', {name: FilterName.ONE_TWO_HOURS});
            expect(oneTwoHoursIntervalButton).toBeVisible();

            const oneTwoHoursIntervalIcon = container.parentElement.querySelector('svg.icon');
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

            const globalSharedActionEmitter = new SharedActionEmitter();
            globalSharedActionEmitter.dispatch({
                type: OPEN_FILTER,
                payload: {filterId: 'delivery-interval'},
            });

            const allIntervalsButton = await screen.findByRole('radio', {name: FilterName.ALL});
            expect(allIntervalsButton).toBeVisible();
            expect(allIntervalsButton).toBeChecked();

            const oneTwoHoursIntervalButton = await screen.findByRole('radio', {name: FilterName.ONE_TWO_HOURS});
            expect(oneTwoHoursIntervalButton).toBeVisible();

            const oneTwoHoursIntervalIcon = container.parentElement.querySelector('svg.icon');
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
