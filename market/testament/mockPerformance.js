// @flow

const defaultMock = {
    ...(global.window ? window.location : {}),
    performance: {
        timing: jest.fn(),
    },
};

export default () => {
    if (typeof window === 'undefined') {
        return () => {
        };
    }

    const {performance} = window;
    delete window.performance;

    window.performance = defaultMock;

    // $FlowFixMe
    Object.defineProperty(window.performance, 'timing', {
        enumerable: true,
        configurable: true,
        get: jest.fn(() => ({
            navigationStart: 0,
        })),
        // $FlowFixMe
        set: defaultMock.assign,
    });

    // $FlowFixMe
    Object.defineProperty(window.performance, 'now', {
        enumerable: true,
        configurable: true,
        get: jest.fn(() => () => 1),
        // $FlowFixMe
        set: defaultMock.assign,
    });

    return () => {
        window.performance = performance;
    };
};

