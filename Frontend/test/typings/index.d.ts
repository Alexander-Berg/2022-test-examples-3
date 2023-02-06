export interface MockBrowser {
    url: jest.Mock;
    yaWaitForPageLoad: Function;
    waitUntil: jest.Mock;
    isVisible: jest.Mock;
    yaShouldSomeBeVisible: jest.Mock;
    addCommand: (handler: Function) => void;
    yaOpenPage: Function;
}
