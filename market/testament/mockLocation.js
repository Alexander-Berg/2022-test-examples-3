// @flow

import {splitTestingUrlByPath} from './url';

const defaultMock = {
    ...(global.window ? window.location : {}),
    reload: jest.fn(),
    assign: jest.fn(),
    replace: jest.fn(),
};

export default (href?: string) => {
    if (typeof window === 'undefined') {
        return () => {
        };
    }

    const {location} = window;
    delete window.location;

    window.location = defaultMock;

    // $FlowFixMe
    Object.defineProperty(window.location, 'href', {
        enumerable: true,
        configurable: true,
        get: jest.fn(() => href || location.href),
        set: defaultMock.assign,
    });

    return () => {
        window.location = location;
    };
};

/**
 * `mockLocation` делает значение window.location.href иммутабельным
 */
export const mockMutableLocation = (initialValue: string) => {
    if (typeof window === 'undefined') {
        return () => {
        };
    }

    const {location} = window;

    delete window.location;
    window.location = {};
    window.location.href = initialValue;

    const [pathname, search] = splitTestingUrlByPath(initialValue);
    window.location.pathname = pathname;
    window.location.search = `_${search}`;

    window.location.reload = () => {};
    window.location.replace = () => {};
    window.location.hash = initialValue.split(/#(.*)/)[1] || '';

    return () => {
        window.location = location;
    };
};
