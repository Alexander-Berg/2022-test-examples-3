/* eslint-disable no-undef */

// eslint-disable-next-line no-use-before-define
import React from 'react';
import '@testing-library/jest-dom';
import {configure} from '@testing-library/react';
import MutationObserver from '@sheerun/mutationobserver-shim';
import {registerAllureReporter} from './catsAllure';

const nativeToLocalString = Number.prototype.toLocaleString;

const mockToLocalString = function (...args) {
    return nativeToLocalString.apply(this, args).replace(/\s/g, ',');
};

if (process.env.CATS_ALLURE) {
    registerAllureReporter();
}

// Мокаем метод `toLocaleString` чтобы отвязаться от настроек локали
// на конкретной машинке, которая будет гонять тесты
jest.spyOn(Number.prototype, 'toLocaleString').mockImplementation(mockToLocalString);

// Компонент не рендерится в jestdom из-за библиотеки InliveSVG
// Которая используеются внутри компонента Icon
jest.mock('react-inlinesvg');
jest.mock('@yandex-market/b2b-components/node_modules/react-inlinesvg');
jest.mock('@yandex-levitan/b2b/components/Icon', () => {
    // eslint-disable-next-line react/prop-types
    const Icon = ({children = <svg width="1" height="1" />, 'data-e2e': dataE2e}) => {
        if (!dataE2e) {
            return children;
        }

        return <span data-e2e={dataE2e}>{children}</span>;
    };

    return {
        __esModule: true,
        default: Icon,
    };
});

jest.retryTimes(1).setTimeout(30000);

// Ошибки в консоль
global.Ya = {Rum: {logError: ({message}, err) => console.error(message, err?.message)}};

// Вместо использования метода screen.getByTestId(key)
// Нужно использовать ctx.getByQuerySelector('[data-e2e="{key}"')
// @see client.next/spec/componentsHelpers.ts
configure({testIdAttribute: 'NOT_USE'});

// Нужен для работы waitFor
// eslint-disable-next-line no-undef
window.MutationObserver = MutationObserver;

if (!window.HTMLElement.prototype.scrollIntoView) {
    window.HTMLElement.prototype.scrollIntoView = function scrollIntoView$stub() {};
}

if (!document.execCommand) {
    document.execCommand = () => true;
    window.getSelection = () => ({
        empty: () => true,
        removeAllRanges: () => true,
    });
}

if (!window.navigator?.clipboard?.writeText) {
    window.navigator.clipboard = {
        async writeText(data) {
            this.value = data;
        },
        async readText() {
            return this.value;
        },
    };
}

// Error: Not implemented: window.scrollTo
Object.defineProperty(window, 'scrollTo', {
    configurable: true,
    enumerable: false,
    value: () => null,
});

// Нужен для работы хука useBreakpoint из левитана
// @see https://jestjs.io/docs/manual-mocks#mocking-methods-which-are-not-implemented-in-jsdom
// eslint-disable-next-line no-undef
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation(query => {
        const [, min, max] = /min-width: (\d+)px.*max-width: (-?\d+)px/.exec(query) || [];
        const width = window.clientWidth;
        const matches = /,/.test(query) ? width >= min || width <= max : width >= min && width <= max;
        return {
            matches,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        };
    }),
});

// Нужно для спискок с бесконечной подгрузкой и всяких штук внутри скролла
// Взято тут https://github.com/thebuilder/react-intersection-observer/blob/master/src/test-utils.ts
const observers = new Map();
// eslint-disable-next-line no-undef
Object.defineProperty(window, 'IntersectionObserver', {
    writable: true,
    value: jest.fn((cb, options = {}) => {
        const item = {
            callback: cb,
            elements: new Set(),
            created: Date.now(),
        };
        const instance = {
            thresholds: Array.isArray(options.threshold) ? options.threshold : [options.threshold ?? 0],
            root: options.root ?? null,
            rootMargin: options.rootMargin ?? '',
            observe: jest.fn(element => {
                item.elements.add(element);
            }),
            unobserve: jest.fn(element => {
                item.elements.delete(element);
            }),
            disconnect: jest.fn(() => {
                observers.delete(instance);
            }),
            takeRecords: jest.fn(),
        };

        observers.set(instance, item);

        return instance;
    }),
});

// Чтобы не ломались @yandex-levitan/b2b/components/Uploader/core/helpers.js
// eslint-disable-next-line no-undef
if (typeof window.URL.createObjectURL === 'undefined') {
    // eslint-disable-next-line no-undef
    window.URL.createObjectURL = () => 'file://test.file';
}
