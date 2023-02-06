jest.mock('puppeteer', () => {
    const page = {
        goto: jest.fn(),
        evaluate: jest.fn((callback, ...args) => callback(...args)),
        on: jest.fn()
    };

    const browser = {
        newPage: () => Promise.resolve(page),
        close: jest.fn()
    };

    return {
        launch: () => Promise.resolve(browser),
        mockedBrowser: browser,
        mockedPage: page
    };
});

jest.mock('../../server/lib/helpers/page-functions', () => ({
    onParticipantsCountChange: jest.fn(() => new Promise(() => {})),
    onJoin: jest.fn(() => Promise.resolve()),
    startStream: jest.fn(),
    setGridView: jest.fn()
}));
