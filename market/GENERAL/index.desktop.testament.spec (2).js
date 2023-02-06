// @flow

// flowlint-next-line untyped-import:off
import {screen} from '@testing-library/dom';

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';

import {resolveCompassMock} from './__mocks__';

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
                nid: 23282393,
                ...params,
            },
        },
    });
}

describe('Widget: AllFilters', () => {
    const WIDGET_PATH = '@self/platform/widgets/content/AllFilters';
    const WIDGET_OPTIONS = () => {
        const {visibleSearchResultMock} = require('./__mocks__');
        return {visibleSearchResultPromise: Promise.resolve(visibleSearchResultMock)};
    };

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

        await jestLayer.backend.runCode(resolveCompassMock => {
            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/project/src/resolvers/compass/resolveCompass'), 'resolveCompass')
                .mockReturnValue(Promise.resolve(resolveCompassMock));

            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/root/src/resolvers/cms/searchConfigation/resolveSearchConfigurationFilters'), 'resolveSearchConfigurationFilters')
                .mockReturnValue(Promise.resolve([]));

            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/root/src/resolvers/resolveIsFashionCategory'), 'resolveIsFashionCategory')
                .mockReturnValue(Promise.resolve(false));
        }, [resolveCompassMock]);

        await jestLayer.doMock(
            require.resolve('@self/project/src/utils/router'),
            () => ({
                buildUrl: () => 'mockedUrl',
                buildURL: () => 'mockedURL',
            })
        );
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
