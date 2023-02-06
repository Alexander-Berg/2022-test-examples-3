import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { GeolocationAPI } from '@yandex-turbo/components/GeolocationControl/GeolocationApi';
import { GeolocationControl, IProps } from '../GeolocationControl';

const geolocationControlProps: IProps = {
    isCurrentLocation: true,
    gpsaveEndpoint: '/gpsave',
    pageUrl: 'https://yandex.ru/pogoda',
    secretKey: 'DEADBEEF',
    lang: 'ru',
    errorMessages: {
        default: 'Не удалось определить местоположение',
        'permission-denied': 'Разрешите геолокацию в настройках браузера',
        'gpsave-error': 'Нет соединения с интернетом',
    },
    ariaLabelsMessages: {
        close: 'Закрыть',
    },
};

jest.mock('@yandex-turbo/core/ajax', () => ({
    get: jest.fn(url => {
        if (/gpsave/.test(url)) {
            return Promise.resolve({});
        }

        return Promise.reject();
    }),
}));

interface IPartialPosition {
    timestamp: number;
    coords: Partial<Coordinates>;
}

// Это нужно, чтобы TS не ругался, когда мы подменяем свойство navigator.geolocation,
// которое помечено как readonly согласно стандартным определениям типов
interface IMutableNavigator {
    geolocation: null | {
        getCurrentPosition: (
            onSuccess: (location: IPartialPosition) => void,
            onError: (error: Partial<PositionError>) => void
        ) => void;
    };
}

function successfulGeoMock(location: IPartialPosition) {
    return {
        getCurrentPosition: jest.fn(successCallback => {
            successCallback(location);
        }),
    };
}

function erroneousGeoMock(error: Partial<PositionError>) {
    return {
        getCurrentPosition: jest.fn((_, errorCallback) => {
            errorCallback(error);
        }),
    };
}

async function nextTick() {
    return new Promise(resolve => setTimeout(resolve));
}

describe('GeolocationControl component', () => {
    const redirect = jest.fn();

    beforeEach(() => {
        redirect.mockReset();
        // @ts-ignore private
        GeolocationAPI.prototype.setLocation = redirect;
    });

    it('should render without crashing', () => {
        const geolocationComponent = shallow(
            <GeolocationControl {...geolocationControlProps} />
        );

        expect(geolocationComponent.length).toEqual(1);
    });

    it('should not render when geolocation is not available', async() => {
        (window.navigator as IMutableNavigator).geolocation = null;

        const geolocationComponent = await mount(
            <GeolocationControl {...geolocationControlProps} />
        );

        expect(geolocationComponent.children().length).toEqual(0);
    });

    it('should redirect after click when geolocation is ok', async() => {
        (window.navigator as IMutableNavigator).geolocation = successfulGeoMock({
            timestamp: Date.now(),
            coords: {
                latitude: 55.55555555,
                longitude: 37.37373737,
                accuracy: 1000,
            },
        });

        const geolocationComponent = await mount(
            <GeolocationControl {...geolocationControlProps} />
        );
        geolocationComponent.find('.geolocation-control__clarify').simulate('click');
        await nextTick();
        expect(redirect).toHaveBeenCalledWith(geolocationControlProps.pageUrl);
    });

    it('should show error message when geolocation failed', async() => {
        (window.navigator as IMutableNavigator).geolocation = erroneousGeoMock({ code: 1 });

        const geolocationComponent = await mount(
            <GeolocationControl {...geolocationControlProps} />
        );
        geolocationComponent.find('.geolocation-control__clarify').simulate('click');
        await nextTick();
        await geolocationComponent.update();
        expect(geolocationComponent.find('.geolocation-control__error-popup').text()).toEqual(
            geolocationControlProps.errorMessages['permission-denied']
        );
        expect(redirect).not.toHaveBeenCalled();
    });

    it('should show error when geolocation coords are too loose', async() => {
        (window.navigator as IMutableNavigator).geolocation = successfulGeoMock({
            timestamp: Date.now(),
            coords: {
                latitude: 55.5,
                longitude: 37.3,
                accuracy: 10000,
            },
        });

        const geolocationComponent = await mount(
            <GeolocationControl {...geolocationControlProps} />
        );
        geolocationComponent.find('.geolocation-control__clarify').simulate('click');
        await nextTick();
        await geolocationComponent.update();
        expect(geolocationComponent.find('.geolocation-control__error-popup').text()).toEqual(
            geolocationControlProps.errorMessages.default
        );
    });

    it('should close error popup by clicking on "close"', async() => {
        (window.navigator as IMutableNavigator).geolocation = erroneousGeoMock({ code: 1 });

        const geolocationComponent = await mount(
            <GeolocationControl {...geolocationControlProps} />
        );
        geolocationComponent.find('.geolocation-control__clarify').simulate('click');
        await nextTick();
        await geolocationComponent.update();
        expect(geolocationComponent.find('.geolocation-control__error-popup').text()).toEqual(
            geolocationControlProps.errorMessages['permission-denied']
        );
        expect(redirect).not.toHaveBeenCalled();
    });
});
