/* eslint-disable mocha/no-skipped-tests */
import * as React from 'react';
import { useHistory } from 'react-router-dom';
import { shallow, ShallowWrapper } from 'enzyme';

import * as UrlManager from '../../lib/url-manager';
import getGeolocation, { GeolocationAPI, GeolocationApiError } from '../../lib/geolocation';
import { initRoutingVariables } from '../../helpers/routingVars';
import { settingsLocationsDeepLink } from '../../constants/weather-app';
import { mockOnce } from '../../__mocks__/exported-const';
import { makeYaConf } from '../../__mocks__/yaConf';
import getMockLocation from '../../__mocks__/location';
import mockI18N from '../../__mocks__/i18n';

import RegionMoscowDistrict from '../../__dumps__/region-moscow-disctrict.json';

import * as HelpersCommon from '../../helpers/common';
import locationRu from './i18n/ru';

import LocationCmp, { Props } from '.';

type Region = Props['region'];
type GeoObject = Props['geoObject'];

const mockLocation = getMockLocation();
const windowOpen = (window.open = jest.fn());
const isTurboAppAsWeatherApp = jest.spyOn(UrlManager, 'isTurboAppAsWeatherApp');

jest.spyOn(HelpersCommon, 'deepLinkTrigger').mockImplementation(jest.fn);
mockI18N('ru', locationRu);

jest.mock('react-router-dom');
jest.mock('@yandex-int/tap-components/StackNavigator');
jest.mock('../../lib/geolocation');
jest.mock('../../lib/rum');
jest.mock('../../lib/metrika');

declare global {
    namespace NodeJS {
        interface Global {
            location: Location & { assign: jest.MockedFunction<typeof location.assign> };
        }
    }
}

describe('components', () => {
    describe('Location', () => {
        const lat = -89.98;
        const lon = 139.27289;
        const geoId = 213;

        const routingVariables = initRoutingVariables({ location: { ...mockLocation } });
        const routingVariablesSearch = initRoutingVariables({ location: { ...mockLocation }, page: 'search' });
        const realLocation = global.location;

        beforeAll(() => {
            delete global.location;
            global.location = { ...realLocation, assign: jest.fn() };
        });

        afterAll(() => {
            global.location = realLocation;
        });

        it('Название точки в море с широтой и долготой на поиске', () => {
            const expected = `${Math.abs(lat)} ю.ш., ${Math.abs(lon)} в.д.`;
            const cmp = shallow(
                <LocationCmp
                    coords={{ lat, lon }}
                    region={{ lat, lon, id: 0, nname: 'это не должно отобразиться' }}
                    routingVariables={routingVariablesSearch}
                    useShortName
                />
            );
            const title = cmp.find('.title');

            expect(cmp.find('.container').hasClass('dark')).toBeTruthy();
            expect(cmp.find('.label').text()).toBe('Уточнить местоположение');
            expect(title.text()).toBe(expected);
            expect(title.name()).toBe('Link');
            expect(title.prop('to')).toBe('/?via=smc');
        });

        it.skip('Название района, когда свежа геолокация', () => {
            const test = (component: ReturnType<typeof shallow>) => {
                expect(component.find('.title').text()).toBe(RegionMoscowDistrict.geoObject.locality.name);
                expect(component.find('.label').text()).toBe(RegionMoscowDistrict.geoObject.district.name);
            };

            const props = {
                region: RegionMoscowDistrict.region as Region,
                geoObject: RegionMoscowDistrict.geoObject as GeoObject,
                coords: RegionMoscowDistrict.coords,
                routingVariables,
                isCurrentLocation: true,
            };

            test(shallow(<LocationCmp {...props} />));
            test(shallow(<LocationCmp {...props} routingVariables={routingVariablesSearch} />));
        });

        it.skip('Название района в абсорбации, когда запрещена геолокация', () => {
            const routingVariablesForHome = initRoutingVariables({
                location: {
                    ...mockLocation,
                    search: `yaConf=${makeYaConf({
                        geoAllowed: 0,
                    })}`,
                },
            });

            const routingVariablesWithQuery = initRoutingVariables({
                location: {
                    ...mockLocation,
                    search: `lat=${lat}&lon=${lon}&yaConf=${makeYaConf({
                        geoAllowed: 0,
                    })}`,
                },
            });

            const props = {
                region: RegionMoscowDistrict.region as Region,
                geoObject: RegionMoscowDistrict.geoObject as GeoObject,
                coords: RegionMoscowDistrict.coords,
                isCurrentLocation: true,
            };

            expect(
                shallow(<LocationCmp {...props} routingVariables={routingVariablesForHome} />)
                    .find('.label')
                    .text()
            ).toBe('Уточнить местоположение');
            expect(
                shallow(<LocationCmp {...props} routingVariables={routingVariablesWithQuery} />)
                    .find('.label')
                    .text()
            ).toBe(RegionMoscowDistrict.geoObject.district.name);
        });

        it('Погода в городе - геолокация протухла', () => {
            const expectedCname = RegionMoscowDistrict.region.cname;
            const cmp = shallow(
                <LocationCmp
                    region={RegionMoscowDistrict.region as Region}
                    geoObject={RegionMoscowDistrict.geoObject as GeoObject}
                    routingVariables={routingVariables}
                />
            );

            expect(cmp.find('.icon').hasClass('no-location')).toBeTruthy();
            expect(cmp.find('.title').text()).toBe(
                `Погода ${expectedCname.preposition} ${expectedCname.prepositional}`
            );
            expect(cmp.find('.label').text()).toBe('Уточнить местоположение');
            expect(cmp.find('.location').prop('onClick')).toBeInstanceOf(Function);
        });

        it('Название на поиске в ПП без geoObject', () => {
            mockOnce(UrlManager, 'isPP', () => true);
            const cmp = shallow(
                <LocationCmp
                    region={RegionMoscowDistrict.region as Region}
                    coords={RegionMoscowDistrict.coords}
                    routingVariables={routingVariablesSearch}
                    isCurrentLocation
                />
            );

            expect(cmp.find('.container').hasClass('dark no-geo')).toBeTruthy();
            expect(cmp.find('.title').text()).toBe(RegionMoscowDistrict.region.preciseName);
            expect(cmp.find('.label').length).toBe(0);
        });

        it('Название без района при фейк-локации', () => {
            const expectedCname = RegionMoscowDistrict.region.cname;
            const routingVariables = initRoutingVariables({
                location: {
                    ...mockLocation,
                    search: `yaConf=${makeYaConf({
                        lat: 11.22,
                        lon: 33.44,
                    })}`,
                },
            });
            const main = shallow(
                <LocationCmp
                    region={RegionMoscowDistrict.region as Region}
                    coords={RegionMoscowDistrict.coords}
                    routingVariables={routingVariables}
                    isCurrentLocation
                />
            );

            expect(main.find('.title').text()).toBe(
                `Погода ${expectedCname.preposition} ${expectedCname.prepositional}`
            );
            expect(main.find('.label').text()).toBe('Уточнить местоположение');

            const search = shallow(
                <LocationCmp
                    region={RegionMoscowDistrict.region as Region}
                    coords={RegionMoscowDistrict.coords}
                    geoObject={RegionMoscowDistrict.region.geoObject}
                    routingVariables={{ ...routingVariables, page: 'search' }}
                    isCurrentLocation
                />
            );

            expect(search.find('.title').text()).toBe(expectedCname.nominative);
            expect(search.find('.label').text()).toBe('Уточнить местоположение');
        });

        it.skip('Название с районом при фейк-локации и координатами в url', () => {
            const routingVariables = initRoutingVariables({
                location: {
                    ...mockLocation,
                    search: `lat=12&lon=55&yaConf=${makeYaConf({
                        geoId,
                    })}`,
                },
            });
            const main = shallow(
                <LocationCmp
                    region={RegionMoscowDistrict.region as Region}
                    coords={RegionMoscowDistrict.coords}
                    geoObject={RegionMoscowDistrict.region.geoObject}
                    routingVariables={routingVariables}
                    isCurrentLocation
                />
            );

            expect(main.find('.title').text()).toBe(RegionMoscowDistrict.geoObject.locality.name);
            expect(main.find('.label').text()).toBe(RegionMoscowDistrict.geoObject.district.name);

            const search = shallow(
                <LocationCmp
                    region={RegionMoscowDistrict.region as Region}
                    coords={RegionMoscowDistrict.coords}
                    geoObject={RegionMoscowDistrict.region.geoObject}
                    routingVariables={{ ...routingVariables, page: 'search' }}
                    isCurrentLocation
                />
            );

            expect(search.find('.title').text()).toBe(RegionMoscowDistrict.geoObject.locality.name);
            expect(search.find('.label').text()).toBe(RegionMoscowDistrict.geoObject.district.name);
        });

        /**
         * Полная логика интеграции описана тут
         * https://h.yandex-team.ru/?https%3A%2F%2Fyandexteam.sharepoint.com%2F%3Ax%3A%2Fs%2Fweather%2FET7KVZrTBchHs0T_W21pVOgBNJIhZ5yuJZ9OLqHeIe_4fQ%3Fe%3DC9ltc4
         *
         * Кратко:
         * * yaConf без подмены, url чист - геолокация через уточнение
         * * yaConf с подменой, url равен подмене - открыть настройки
         * * yaConf с подменой, url не равен подмене - перейти на подмену
         */
        it('Уточнение в абсорбации - настройки', () => {
            global.location.assign.mockClear();

            const routingVariables = initRoutingVariables({
                location: {
                    ...mockLocation,
                    search: `?yaConf=${makeYaConf({ lat, lon, geoId })}`,
                },
            });

            const props = {
                region: RegionMoscowDistrict.region as Region,
                coords: RegionMoscowDistrict.coords,
                routingVariables,
            };

            const main = shallow(<LocationCmp {...props} isCurrentLocation />);
            main.find('.location').simulate('click');

            expect(HelpersCommon.deepLinkTrigger).toBeCalledWith(settingsLocationsDeepLink);

            main.setProps({
                ...props,
                routingVariables: {
                    ...routingVariables,
                    params: { slug: `${geoId}` },
                    values: { ...routingVariables.values, lat, lon },
                },
            });

            expect(main.find('.label').text()).toBe('Уточнить местоположение');

            main.find('.location').simulate('click');

            expect(HelpersCommon.deepLinkTrigger).toBeCalledWith(settingsLocationsDeepLink);
            expect(HelpersCommon.deepLinkTrigger).toBeCalledTimes(2);

            const mainWithAnotherUseRefValue = shallow(
                <LocationCmp
                    {...props}
                    routingVariables={{
                        ...initRoutingVariables({
                            location: {
                                ...mockLocation,
                                search: `?yaConf=${makeYaConf({ geoId })}`,
                            },
                            page: 'main',
                        }),
                    }}
                    geoObject={RegionMoscowDistrict.region.geoObject}
                    isCurrentLocation
                />
            );

            // в абсорбации, при заданной подмене в yaConf и отсутствии lat/lon, "уточнить" вместо района
            expect(mainWithAnotherUseRefValue.find('.label').text()).toBe('Уточнить местоположение');
        });

        it('Уточнение в абсорбации - переход на yaConf', () => {
            const history = useHistory();
            const historyPush = history.push as jest.MockedFunction<typeof history.push>;
            const historyReplace = history.replace as jest.MockedFunction<typeof history.replace>;

            historyPush.mockClear();
            historyReplace.mockClear();

            const search = `?yaConf=${makeYaConf({ lat, lon, geoId })}`;
            const routingVariables = initRoutingVariables({
                params: { slug: `${geoId - 1}` },
                location: {
                    ...mockLocation,
                    search,
                },
            });

            const mainPageUrl = `/${geoId}${search}&lat=${lat}&lon=${lon}`;

            const props = {
                region: RegionMoscowDistrict.region as Region,
                coords: RegionMoscowDistrict.coords,
                routingVariables,
            };

            const main = shallow(<LocationCmp {...props} />);

            main.find('.location').simulate('click');

            main.setProps({
                ...props,
                routingVariables: {
                    ...routingVariables,
                    page: 'main',
                },
            });

            main.find('.location').simulate('click');

            expect(historyPush).toBeCalledTimes(1);
            expect(historyPush).toBeCalledWith(mainPageUrl);
            expect(historyReplace).toBeCalledTimes(1);
            expect(historyReplace).toBeCalledWith(mainPageUrl);
        });

        it('Уточнение в абсорбации - getCurrentPosition', () => {
            const history = useHistory();
            const historyPush = history.push as jest.MockedFunction<typeof history.push>;
            const historyReplace = history.replace as jest.MockedFunction<typeof history.replace>;
            const hasGeolocation = GeolocationAPI.hasGeolocation as jest.MockedFunction<
                typeof GeolocationAPI.hasGeolocation
            >;

            historyPush.mockClear();
            historyReplace.mockClear();
            hasGeolocation.mockClear();

            const search = `?yaConf=${makeYaConf({})}&lat=${lat}&lon=${lon}`;
            const routingVariables = initRoutingVariables({
                params: { slug: `${geoId}` },
                location: {
                    ...mockLocation,
                    search,
                },
            });

            const props = {
                region: RegionMoscowDistrict.region as Region,
                coords: RegionMoscowDistrict.coords,
                routingVariables,
            };

            const main = shallow(<LocationCmp {...props} />);

            main.find('.location').simulate('click');

            expect(historyPush).toBeCalledTimes(0);
            expect(historyReplace).toBeCalledTimes(0);
            /**
             * тут достаточно проверить обращение к методам геолокаци, а не замене адреса
             */
            expect(getGeolocation).toBeCalledTimes(1);
        });

        type TestPopupArgs = {
            popup: ShallowWrapper;
            btnAmount: number;
            btnChooseManually?: number;
            btnChooseManuallyInSettings?: number;
            btnHelp?: number;
            history: ReturnType<typeof useHistory>;
        };
        const testPopup = ({
            popup,
            btnAmount,
            btnChooseManually,
            btnChooseManuallyInSettings,
            btnHelp,
            history,
        }: TestPopupArgs) => {
            const historyPush = history.push as jest.MockedFunction<typeof history.push>;

            historyPush.mockClear();
            windowOpen.mockClear();
            const buttons = popup.find('.button');
            expect(buttons.length).toBe(btnAmount);

            if (btnChooseManually !== undefined) {
                buttons.at(btnChooseManually).simulate('click');

                expect(historyPush).toBeCalledTimes(1);
                expect(historyPush.mock.calls[0][0]).toMatch(/^\/search\?via=ge/);
            }

            if (btnHelp !== undefined) {
                buttons.at(btnHelp).simulate('click');
                expect(windowOpen).toBeCalledTimes(1);
                expect(windowOpen.mock.calls[0][0]).toMatch(/support.*\.html/);
            }

            if (btnChooseManuallyInSettings !== undefined) {
                buttons.at(btnChooseManuallyInSettings).simulate('click');
                expect(HelpersCommon.deepLinkTrigger).toBeCalledWith(settingsLocationsDeepLink);
            }
        };

        // проверен тольк кейс "гео запрещено": разные кнопки на главной и поиске, их действия
        it.skip('Ошибка геолокации', async function() {
            const getGeolocationMock = getGeolocation as jest.MockedFunction<typeof getGeolocation>;
            const history = useHistory();

            const routingVariables = initRoutingVariables({
                params: { slug: `${geoId}` },
                location: {
                    ...mockLocation,
                },
            });

            const props = {
                region: RegionMoscowDistrict.region as Region,
                coords: RegionMoscowDistrict.coords,
                routingVariables,
            };

            const main = shallow(<LocationCmp {...props} />);

            getGeolocationMock.mockRejectedValueOnce({ error: GeolocationApiError.PermissionDenied });

            main.find('.location').simulate('click');

            await new Promise(resolve => setTimeout(resolve, 10));
            // на главной должно быть 2 кнопки, первая ведет на поиск, вторая на помощь
            testPopup({
                popup: main.find('Popup').dive(),
                history,
                btnAmount: 2,
                btnChooseManually: 0,
                btnHelp: 1,
            });

            main.setProps({
                ...props,
                routingVariables: {
                    ...routingVariables,
                    page: 'search',
                },
            });

            getGeolocationMock.mockRejectedValueOnce({ error: GeolocationApiError.PermissionDenied });

            main.find('.location').simulate('click');

            await new Promise(resolve => setTimeout(resolve, 10));

            // на поиске должна быть только кнопка с помощью
            testPopup({
                history,
                popup: main.find('Popup').dive(),
                btnAmount: 1,
                btnHelp: 0,
            });
        });

        // в абсорбации не убирается кнопка "задать вручную", но вместо ухода на поиск уводит в настройки
        it.skip('Ошибка геолокации - абсорбация', async function() {
            const getGeolocationMock = getGeolocation as jest.MockedFunction<typeof getGeolocation>;
            const history = useHistory();

            isTurboAppAsWeatherApp.mockReturnValue(true);

            const routingVariables = initRoutingVariables({
                params: { slug: undefined },
                location: {
                    ...mockLocation,
                },
            });

            const props = {
                region: RegionMoscowDistrict.region as Region,
                coords: RegionMoscowDistrict.coords,
                routingVariables,
            };

            const main = shallow(<LocationCmp {...props} />);

            getGeolocationMock.mockRejectedValueOnce({ error: GeolocationApiError.PermissionDenied });
            main.find('.location').simulate('click');

            await new Promise(resolve => setTimeout(resolve, 10));
            // на главной должно быть 2 кнопки, первая ведет в настройки, вторая на помощь
            testPopup({
                popup: main.find('Popup').dive(),
                history,
                btnAmount: 2,
                btnChooseManuallyInSettings: 0,
                btnHelp: 1,
            });

            main.setProps({
                ...props,
                routingVariables: {
                    ...routingVariables,
                    page: 'search',
                },
            });

            getGeolocationMock.mockRejectedValueOnce({ error: GeolocationApiError.PermissionDenied });

            main.find('.location').simulate('click');

            await new Promise(resolve => setTimeout(resolve, 10));

            // на поиске должна быть кнопка с помощью и уточнением в настройках
            testPopup({
                popup: main.find('Popup').dive(),
                history,
                btnAmount: 2,
                btnChooseManuallyInSettings: 0,
                btnHelp: 1,
            });

            isTurboAppAsWeatherApp.mockRestore();
        });
    });
});
