import { renderHook, act } from '@testing-library/react-hooks';
import { useDispatch } from 'react-redux';

import * as UrlManager from '../lib/url-manager';
import { GeolocationAPI } from '../lib/geolocation';
import { initRoutingVariables } from '../helpers/routingVars';
import { GP_FRESHNESS } from '../helpers/gpauto';
import getMockLocation from '../__mocks__/location';
import useFetchGeolocatedData, { HookProps } from './useFetchGeolocatedData';

const mockLocation = getMockLocation();
const isTurboAppAsWeatherApp = jest.spyOn(UrlManager, 'isTurboAppAsWeatherApp');
// eslint-disable-next-line react-hooks/rules-of-hooks
const dispatch = useDispatch() as jest.MockedFunction<ReturnType<typeof useDispatch>>;
const isGeolocationPermitted = jest.spyOn(GeolocationAPI, 'isGeolocationPermitted');
const cookieGet = jest.spyOn(document, 'cookie', 'get');
jest.spyOn(GeolocationAPI, 'verifyCoords').mockImplementation(coords => Promise.resolve(coords));
jest.spyOn(GeolocationAPI, 'gpsave').mockImplementation(coords => Promise.resolve(coords));

jest.mock('react-redux');
jest.mock('@yandex-int/tap-components/StackNavigator', () => ({
    useScreenVisible: jest.fn(() => true)
}));
jest.mock('../lib/rum');
jest.mock('../lib/metrika');
jest.mock('../helpers/isHomeLocation', () => jest.fn(() => true));

const MOCK_LOCATION = {
    coords: { latitude: 50, longitude: 30, accuracy: 0 },
};

Object.defineProperty(MOCK_LOCATION, 'timestamp', {
    get(): number {
        return new Date().getTime();
    }
});

const mockGeolocation = {
    mockLocation: jest.fn<typeof MOCK_LOCATION & { timestamp?: number }, never>(() => MOCK_LOCATION),
    mockMinGetCurrentPositionDelay: jest.fn(() => 1),
    getCurrentPosition: jest.fn((resolve, reject, { timeout }) => {
        const location: typeof MOCK_LOCATION = mockGeolocation.mockLocation();
        const delay: number = mockGeolocation.mockMinGetCurrentPositionDelay();
        const callback = () => {
            if (delay !== undefined && timeout !== undefined && delay > timeout) {
                return reject({ message: 'test geolocation error: timeout', code: 3 });
            }

            if (location) {
                return resolve(location);
            }

            reject(new Error('test geolocation error'));
        };

        if (delay === undefined) {
            return callback();
        }

        setTimeout(callback, delay);
    }),
    watchPosition: jest.fn()
};

const describeWrap = (run: jest.EmptyFunction) =>
    describe('hooks', () => {
        describe('useFetchGeolocatedData', run);
    });

describeWrap(() => {
    // we need to update react to 1.6.9 at least
    // https://github.com/testing-library/react-hooks-testing-library/issues/14#issuecomment-475096681
    const consoleErr = console.error.bind(console);
    // @ts-ignore
    const geolocation = global.navigator.geolocation;
    const routingVariables = initRoutingVariables({ location: { ...mockLocation } });

    beforeAll(() => {
        // @ts-ignore
        global.navigator.geolocation = mockGeolocation;
    });

    beforeEach(async() => {
        dispatch.mockClear();
        isGeolocationPermitted.mockClear();
        mockGeolocation.getCurrentPosition.mockClear();
        cookieGet.mockReset();
        localStorage.removeItem('geolocation');
        global.console.error = () => {};
        GeolocationAPI.positionResolver = undefined;
    });

    afterEach(async() => {
        await new Promise(resolve => {
            setTimeout(() => {
                global.console.error = consoleErr;
                resolve();
            }, 1);
        });
    });

    afterAll(() => {
        // @ts-ignore
        global.navigator.geolocation = geolocation;
    });

    const runAndCheckHook = async(props: HookProps, checks: {
        geolocationError?: string;
        popupVisible?: boolean;
        dispatchedTimes?: number;
        geoCalledTimes?: number;
    } = {}) => {
        const { result } = renderHook(() => useFetchGeolocatedData(props));

        await new Promise(resolve => setTimeout(resolve, 5));
        await GeolocationAPI.positionResolver?.finally(() => Promise.resolve());
        act(() => {});

        expect(result.current.geolocationError).toBe(checks.geolocationError);
        expect(result.current.hidePopup).toBeInstanceOf(Function);
        expect(result.current.popupVisible).toBe(Boolean(checks.popupVisible));
        expect(dispatch.mock.calls).toHaveLength(
            checks.dispatchedTimes === undefined ? 1 : checks.dispatchedTimes
        );
        expect(mockGeolocation.getCurrentPosition.mock.calls).toHaveLength(
            checks.geoCalledTimes === undefined ? 2 : checks.geoCalledTimes
        );
    };

    it('Абсорбация: не автоуточняться без gps/permission', async() => {
        isTurboAppAsWeatherApp.mockReturnValue(true);
        isGeolocationPermitted.mockResolvedValue(false);

        const props = { routingVariables, expFlags: {}, params: {} };

        /**
         * хук умеет сообщать о возникшей ошибке геолокации
         * однако в верстке это может быть не отражено
         */
        await runAndCheckHook(props, {
            geolocationError: 'permission-denied',
            popupVisible: true,
            geoCalledTimes: 0
        });
    });

    it('Абсорбация: не автоуточняться в экспе-запрете', async() => {
        isTurboAppAsWeatherApp.mockReturnValue(true);
        isGeolocationPermitted.mockResolvedValue(true);

        const props = { routingVariables, expFlags: {
            spa_abs_disable_autoprecise: true
        }, params: {} };

        await runAndCheckHook(props, {
            geoCalledTimes: 0
        });
    });

    it('Абсорбация: автоуточняемся', async() => {
        isTurboAppAsWeatherApp.mockReturnValue(true);
        isGeolocationPermitted.mockResolvedValue(true);

        const props = { routingVariables, expFlags: {}, params: {} };

        await runAndCheckHook(props);
    });

    it('Абсорбация: автоуточняемся несмотря на свежую и точную куку yp.gpauto', async() => {
        const nowUnix = Math.floor(Date.now() / 1000);

        cookieGet.mockReturnValue(`gdpr=0; yp=${nowUnix}.ygo.213:54#${nowUnix}.gpauto.56_838013:60_597466:100:3:${nowUnix}#${nowUnix}.ygu.0; yandex_gid=54;`);

        isTurboAppAsWeatherApp.mockReturnValue(true);
        isGeolocationPermitted.mockResolvedValue(true);

        const props = { routingVariables, expFlags: {}, params: {} };

        await runAndCheckHook(props);
    });

    it('Абсорбация: автоуточняемся на координаты, возраст которых 0-30мин', async() => {
        isTurboAppAsWeatherApp.mockReturnValue(true);
        isGeolocationPermitted.mockResolvedValue(true);

        const mockLocation = {
            ...MOCK_LOCATION,
            timestamp: Date.now() - 1000 * 60 * 20
        };

        mockGeolocation.mockLocation
            .mockReturnValueOnce(mockLocation)
            .mockReturnValueOnce(mockLocation);

        const props = { routingVariables, expFlags: {}, params: {
            maxVerifyAge: 30 * 60 * 1000
        } };

        await runAndCheckHook(props);
    });

    it('Не уточняемся без permission', async() => {
        isTurboAppAsWeatherApp.mockReturnValue(false);
        isGeolocationPermitted.mockResolvedValue(false);

        const props = { routingVariables, expFlags: {}, params: {} };

        await runAndCheckHook(props, {
            geoCalledTimes: 0,
            geolocationError: 'permission-denied',
            popupVisible: true
        });
    });

    it('Уточняемся с permission и без куки yp.gpauto', async() => {
        isTurboAppAsWeatherApp.mockReturnValue(false);
        isGeolocationPermitted.mockResolvedValue(true);

        const props = { routingVariables, expFlags: {}, params: {} };

        await runAndCheckHook(props);
    });

    it('Ошибка уточнения при браузерной локации старше 30мин', async() => {
        isTurboAppAsWeatherApp.mockReturnValue(false);
        isGeolocationPermitted.mockResolvedValue(true);

        const mockLocation = {
            ...MOCK_LOCATION,
            timestamp: Date.now() - 1000 * 60 * 31
        };

        mockGeolocation.mockLocation
            .mockReturnValueOnce(mockLocation)
            .mockReturnValueOnce(mockLocation);

        const props = { routingVariables, expFlags: {}, params: {} };

        await runAndCheckHook(props, {
            geolocationError: 'irrelevant-coords',
            popupVisible: true
        });
    });

    it('Уточняемся при тухлой куке yp.gpauto', async() => {
        const nowUnix = Math.floor(Date.now() / 1000);
        const outdatedUnix = nowUnix - Math.floor(GP_FRESHNESS / 1000) - 100;

        cookieGet.mockReturnValue(`gdpr=0; yp=${nowUnix}.ygo.213:54#${nowUnix}.gpauto.56_838013:60_597466:50:3:${outdatedUnix}#${nowUnix}.ygu.0; yandex_gid=54;`);
        isTurboAppAsWeatherApp.mockReturnValue(false);
        isGeolocationPermitted.mockResolvedValue(true);

        const props = { routingVariables, expFlags: {}, params: {} };

        await runAndCheckHook(props);
    });

    it('Уточняемся при свежей неточной куке yp.gpauto', async() => {
        const nowUnix = Math.floor(Date.now() / 1000);
        const wrongPrecision = 10000;

        cookieGet.mockReturnValue(`gdpr=0; yp=${nowUnix}.ygo.213:54#${nowUnix}.gpauto.56_838013:60_597466:${wrongPrecision}:3:${nowUnix}#${nowUnix}.ygu.0; yandex_gid=54;`);
        isTurboAppAsWeatherApp.mockReturnValue(false);
        isGeolocationPermitted.mockResolvedValue(true);

        const props = { routingVariables, expFlags: {}, params: {} };

        await runAndCheckHook(props);
    });

    it('Не уточняемся при свежей и точной куке yp.gpauto', async() => {
        const nowUnix = Math.floor(Date.now() / 1000);

        cookieGet.mockReturnValue(`gdpr=0; yp=${nowUnix}.ygo.213:54#${nowUnix}.gpauto.56_838013:60_597466:100:3:${nowUnix}#${nowUnix}.ygu.0; yandex_gid=54;`);
        isTurboAppAsWeatherApp.mockReturnValue(false);
        isGeolocationPermitted.mockResolvedValue(true);

        const props = { routingVariables, expFlags: {}, params: {} };

        await runAndCheckHook(props, { geoCalledTimes: 0 });
    });
});
