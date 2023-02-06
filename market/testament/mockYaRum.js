// @flow

const defaultMock = {
    ...(global.window ? window.Ya : {}),
    Rum: {
        getTime: jest.fn(),
        time: jest.fn(),
        timeEnd: jest.fn(),
        sendTimeMark: jest.fn(),
    },
};

export default () => {
    if (typeof window === 'undefined') {
        return () => {};
    }

    const {Ya} = window;
    delete window.Ya;

    window.Ya = defaultMock;

    return () => {
        window.Ya = Ya;
    };
};

