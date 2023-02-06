import * as React from 'react';
import { useSelector as importedUseSelector } from 'react-redux';
import { shallow, ShallowWrapper } from 'enzyme';

import * as UrlManager from '../../lib/url-manager';
import { initRoutingVariables } from '../../helpers/routingVars';

import getMockLocation from '../../__mocks__/location';
import mockI18N from '../../__mocks__/i18n';
import RegionMoscowDistrict from '../../__dumps__/region-moscow-disctrict.json';

import locationRu from './i18n/ru';

import MainNavigation, { Props } from '.';

const useSelector = importedUseSelector as jest.MockedFunction<typeof importedUseSelector>;

jest.mock('react-redux');
jest.mock('../../lib/rum');
jest.mock('../../lib/metrika');
jest.mock('@yandex-int/react-baobab-logger');

const isTurboAppAsWeatherApp = jest.spyOn(UrlManager, 'isTurboAppAsWeatherApp');

mockI18N('ru', locationRu);

describe('components', () => {
    describe('MainNavigation', () => {
        beforeAll(() => {
            useSelector.mockReturnValue(Promise.resolve({}));
        });
        afterAll(() => {
            useSelector.mockReset();
        });

        it('Обычное поведение', () => {
            const toBeNormal = (element: ShallowWrapper) => {
                expect(element.children().length).toEqual(3);

                const [navigation, location, favorite] = [
                    element.childAt(0),
                    /**
                     * можно искать через element.find('Location'), а не выбирать по структуре,
                     * но позиционированный тест будет пожестче и, кажется, полезнее –
                     * означает, что все на своих местах
                     */
                    element.childAt(1).childAt(0),
                    element.childAt(2).childAt(0),
                ];

                expect(navigation.name()).toBe('Link');
                expect(location.name()).toBe('Location');
                expect(favorite.name()).toBe('Favorite');
            };

            let cmp = shallow(
                <MainNavigation
                    region={RegionMoscowDistrict.region as Props['region']}
                    routingVariables={initRoutingVariables({ location: getMockLocation() })}
                />
            );

            toBeNormal(cmp);

            cmp = shallow(
                <MainNavigation
                    region={RegionMoscowDistrict.region as Props['region']}
                    routingVariables={initRoutingVariables({
                        location: {
                            ...getMockLocation(),
                            search: 'lat=55&lon=77&startScreen=1',
                        },
                    })}
                />
            );

            toBeNormal(cmp);
        });

        it('Абсорбация', () => {
            isTurboAppAsWeatherApp.mockReturnValue(true);

            const toBeSearch = (element: ShallowWrapper) => {
                expect(element.name()).toBe('Link');
                expect(element.prop('to')).toMatch(/^\/search\?via=hs/);
                expect(element.prop('className')).toMatch(/side/);
            };

            const toBeBackward = (element: ShallowWrapper) => {
                expect(element.name()).toBe('BackwardButton');
                expect(element.prop('backwardLink')).toMatch(/^\/\?via=mb/);
                expect(element.prop('className')).toMatch(/side/);
            };

            const toBeLocation = (element: ShallowWrapper) => {
                expect(element.prop('className')).toMatch('center');
                expect(element.children().length).toEqual(1);
                expect(element.childAt(0).name()).toBe('Location');
            };

            const toBeFavorite = (element: ShallowWrapper) => {
                expect(element.childAt(0).name()).toBe('Favorite');
            };

            let cmp = shallow(
                <MainNavigation
                    region={RegionMoscowDistrict.region as Props['region']}
                    routingVariables={initRoutingVariables({ location: getMockLocation() })}
                />
            );
            // всегда 3 элемента
            // домашняя локация - кнопка настроек
            expect(cmp.name()).toBe('div');
            expect(cmp.prop('className')).toBe('container');
            expect(cmp.children().length).toEqual(3);
            toBeSearch(cmp.childAt(0));
            toBeLocation(cmp.childAt(1));
            toBeFavorite(cmp.childAt(2));

            cmp = shallow(
                <MainNavigation
                    region={RegionMoscowDistrict.region as Props['region']}
                    routingVariables={initRoutingVariables({
                        location: {
                            ...getMockLocation(),
                            search: 'lat=55&lon=77',
                        },
                    })}
                />
            );
            // чужая локация - кнопка избранного и назад
            expect(cmp.children().length).toBe(3);
            toBeBackward(cmp.childAt(0));
            toBeLocation(cmp.childAt(1));
            toBeFavorite(cmp.childAt(2));

            cmp = shallow(
                <MainNavigation
                    region={RegionMoscowDistrict.region as Props['region']}
                    routingVariables={initRoutingVariables({
                        location: {
                            ...getMockLocation(),
                            search: 'lat=55&lon=77&startScreen=1',
                        },
                    })}
                />
            );
            // своя локация через ?startScreen=1 - кнопка настроек
            expect(cmp.children().length).toBe(3);
            toBeSearch(cmp.childAt(0));
            toBeLocation(cmp.childAt(1));
            toBeFavorite(cmp.childAt(2));

            isTurboAppAsWeatherApp.mockRestore();
        });
    });
});
