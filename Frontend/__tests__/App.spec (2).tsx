// @ts-nocheck
import * as React from 'react';
import { mount } from 'enzyme';
import * as redux from 'react-redux';

import * as overrideHostModule from '~/libs/set-override-host';
import * as initialState from './stors/farkop_main.json';
import { App } from '../App';

jest.mock('~/utils/baobab/BaobabInjectorButton', () => ({ BaobabInjectorButton: () => 'BaobabInjectorButton' }));
jest.mock('~/components/Navigation', () => ({ Navigation: () => 'Navigation' }));
jest.mock('~/components/EcomFooter/EcomFooter', () => ({ EcomFooter: () => 'EcomFooter' }));
jest.mock('react-router-dom', () => {
    return {
        useLocation: () => ({}),
        useHistory: () => ({
            createHref: () => '/turbo/farkop.ru?page_type=main&ecommerce_main_page_preview=1&morda=1&text=farkop.ru%2Fyandexturbocatalog%2F&itd=1&parent_reqid=123456',
        }),
        withRouter: component => component,
    };
});

describe('AppPresenter', function() {
    beforeEach(function() {
        //@see https://jestjs.io/docs/en/manual-mocks#mocking-methods-which-are-not-implemented-in-jsdom
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation(query => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(), // deprecated
                removeListener: jest.fn(), // deprecated
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
            })),
        });
    });

    it('вызывается подмена домена', function() {
        const useDispatchSpy = jest.spyOn(redux, 'useDispatch');
        const useSelectorSpy = jest.spyOn(redux, 'useSelector');
        const setOmniboxHostSpy = jest.spyOn(overrideHostModule, 'setOmniboxHost');

        useDispatchSpy.mockReturnValue(jest.fn());
        useSelectorSpy.mockImplementation(selector => selector(initialState));
        setOmniboxHostSpy.mockImplementation(jest.fn());

        mount(<App />);

        expect(setOmniboxHostSpy).toBeCalledWith(
            {
                keyUrls: [
                    'http://localhost/turbo/farkop.ru?page_type=main&ecommerce_main_page_preview=1&morda=1&text=farkop.ru%2Fyandexturbocatalog%2F&itd=1&parent_reqid=123456',
                    'http://localhost/turbo/farkop.ru?page_type=main&ecommerce_main_page_preview=1&morda=1&text=farkop.ru%2Fyandexturbocatalog%2F&parent_reqid=123456',
                    'http://localhost/turbo/farkop.ru?page_type=main&ecommerce_main_page_preview=1&morda=1&text=farkop.ru%2Fyandexturbocatalog%2F',
                ],
                displayUrl: 'http://localhost/turbo/farkop.ru?page_type=main&ecommerce_main_page_preview=1&morda=1&text=farkop.ru%2Fyandexturbocatalog%2F',
                displayHost: 'farkop.ru',
            }
        );

        useDispatchSpy.mockClear();
        useSelectorSpy.mockClear();
        setOmniboxHostSpy.mockClear();
    });
});
