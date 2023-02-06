import React from 'react';
import { StaticRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import { ApolloProvider } from '@apollo/client/react';

import gqlClient from './graphql/client-web';
import getStore, { dataToState } from './redux';
import { localization, LocalizationProvider } from './lib/i18n';
import { initRoutingVariables } from './helpers/routingVars';
import { getPathMatch } from './helpers/getPathMatch';
import { tests, getTestsConfig } from './__mocks__/app.common';
import { Language } from './types';

import MainScreen from './screens/MainScreen/Component';
import DetailsScreen from './screens/DetailsScreen/Component';
import MonthScreen from './screens/MonthScreen/Component';
import SearchScreen from './screens/SearchScreen/Component';
import RegionScreen from './screens/RegionScreen/Component';
import MapsScreen from './screens/MapsScreen/Component';
import App from './app';

const Screens = {
    main: MainScreen,
    details: DetailsScreen,
    month: MonthScreen,
    search: SearchScreen,
    region: RegionScreen,
    maps: MapsScreen
};

jest.mock('./helpers/lazy', () => {
    const lazy = jest.requireActual('./helpers/lazy');

    return {
        ...lazy,
        // @ts-ignore
        loadable: promise => lazy.loadable(promise, { sleep: 0, delay: 0 }),
        // @ts-ignore
        lazy: arg => arg
    };
});
jest.mock('./lib/rum');
jest.mock('react', () => {
    // mock => requireActual убирает invariant-ошибки
    const react = jest.requireActual('react');

    // useLayoutEffect при dev-серверном рендере гадит в консоль
    return { ...react, useLayoutEffect: react.useEffect };
});
jest.unmock('react-redux');

describe('App', () => {
    /**
     * Проверяем, что
     * * верхнеуровнево не отпали полифиллы
     * * есть рендер скелетонов
     *
     * Несмотря на то, что в тестах прокидываются данные
     * Будут отрендерены скелетоны, так как jest не резолвит webpack.import(chunk.js)
     */
    let originals = {
        Array: {
            prototype: {
                includes: Array.prototype.includes,
                fill: Array.prototype.fill,
                find: Array.prototype.find
            }
        },
        Object: {
            entries: Object.entries
        },
        // @ts-ignore
        fetch: global.fetch,
        // @ts-ignore
        URLSearchParams: global.URLSearchParams
    };

    const preload = ({ lang }: { lang: Language }) => Promise.all([
        new Promise(resolve => resolve(Promise.all(require('./polyfill-app').default))),
        localization.loadI18n(lang)
    ]);

    beforeAll(() => {
        // @ts-ignore
        delete global.Array.prototype.includes;
        // @ts-ignore
        delete global.Array.prototype.fill;
        // @ts-ignore
        delete global.Array.prototype.find;
        // @ts-ignore
        delete global.Object.entries;
        // @ts-ignore
        delete global.fetch;
        // @ts-ignore
        delete global.URLSearchParams;
    });

    afterAll(() => {
        global.Array.prototype.includes = originals.Array.prototype.includes;
        global.Array.prototype.fill = originals.Array.prototype.fill;
        global.Array.prototype.find = originals.Array.prototype.find;
        global.Object.entries = originals.Object.entries;
        // @ts-ignore
        global.fetch = originals.fetch;
        // @ts-ignore
        global.URLSearchParams = originals.URLSearchParams;
    });

    getTestsConfig(tests).forEach(({ testName, location, data, lang, screen }) => {
        it(testName, async() => {
            await preload({ lang });

            const pathMatch = getPathMatch(location, {});
            let state = undefined;

            const routingVariables = initRoutingVariables({ ...pathMatch });
            const stateInitializer = pathMatch.slice && dataToState[pathMatch.slice];

            if (data && stateInitializer) {
                // @ts-ignore
                state = stateInitializer(routingVariables, data);
            }

            const skeleton = shallow(
                <Provider store={getStore()}>
                    <LocalizationProvider>
                        <StaticRouter location={location}>
                            <App />
                        </StaticRouter>
                    </LocalizationProvider>
                </Provider>
            );

            skeleton.render();

            expect(skeleton).toBeTruthy();

            const Screen = screen && Screens[screen];

            if (Screen) {
                const live = shallow(
                    <ApolloProvider client={gqlClient}>
                        <Provider store={getStore(state)}>
                            <LocalizationProvider>
                                <StaticRouter location={location}>
                                    <Screen
                                        routingVariables={routingVariables}
                                        data={data.data}
                                        options={data.options}
                                        skeleton={<div>Test skeleton</div>}
                                    />
                                </StaticRouter>
                            </LocalizationProvider>
                        </Provider>
                    </ApolloProvider>
                );

                live.render();

                expect(live).toBeTruthy();
            }
        });
    });
});
