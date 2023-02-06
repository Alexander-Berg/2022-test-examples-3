import './global';
import { makeEventStub } from '../api/stub';
import { apiProvider, WithMeta } from '../api/helpers';
import ExtensionEvent from '../api/ExtensionEvent';
import { isGreaterThanOrEqual } from '../BrowserVersion';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const messenger = (global as any).yandex.messenger;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const navigator = (global as any).navigator;

describe('#api provider', () => {
    afterEach(() => {
        Object.keys(messenger).forEach(key => {
            delete messenger[key];
        });
    });

    describe('#api.event', () => {
        it('should be available if event in the scope ', () => {
            const { event } = apiProvider('messenger');

            class TestAPI {
                @event onViewHidden: ExtensionEvent;
            }

            const onViewHidden = makeEventStub('onViewHidden');
            const spy = spyOn(onViewHidden, 'addListener');

            messenger.onViewHidden = onViewHidden;

            const testAPI = new TestAPI();

            testAPI.onViewHidden.addListener(() => {});

            expect(spy).toBeCalledTimes(1);
        });

        it('should stubbed if event not in the scope', () => {
            const { event } = apiProvider('messenger');

            class TestAPI {
                @event onViewHidden: ExtensionEvent;
            }

            const testAPI = new TestAPI() as WithMeta<TestAPI>;

            expect(testAPI.onViewHidden.notSupported).toBeTruthy();
            expect(testAPI.onViewHidden.addListener).toBeDefined();
            expect(testAPI.onViewHidden.removeListener).toBeDefined();
        });
    });

    describe('#api', () => {
        it('should be available if method in the scope', () => {
            const { handle } = apiProvider('messenger');

            class TestAPI {
                @handle testMethod: () => void;
            }

            const testMethod = jest.fn();

            messenger.testMethod = testMethod;

            const testAPI = new TestAPI() as WithMeta<TestAPI>;

            testAPI.testMethod();

            expect(testMethod).toBeCalledTimes(1);
        });

        it('should be stubbed if method not in the scope', () => {
            const { handle } = apiProvider('messenger');

            class TestAPI {
                @handle testMethod: () => void;
            }

            const testAPI = new TestAPI() as WithMeta<TestAPI>;

            testAPI.testMethod();

            expect(testAPI.testMethod.notSupported).toBeTruthy();
            expect(typeof testAPI.testMethod).toBe('function');
        });
    });

    describe('#api.ifVersion', () => {
        it('should be available if bro version > 20.4.0', () => {
            const testMethod = jest.fn();

            navigator.userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.41 YaBrowser/20.5.0.0 Yowser/2.5 Safari/537.36';
            messenger.testMethod = testMethod;

            const { handle } = apiProvider('messenger');

            class TestAPI {
                @handle.availableIfVersion(isGreaterThanOrEqual(20, 4, 0))
                testMethod: () => void;
            }

            const testAPI = new TestAPI() as WithMeta<TestAPI>;

            testAPI.testMethod();

            expect(testMethod).toBeCalledTimes(1);
        });

        it('should be stubbed if bro version < 20.4.0', () => {
            const testMethod = jest.fn();

            navigator.userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.41 YaBrowser/19.5.0.0 Yowser/2.5 Safari/537.36';
            messenger.testMethod = testMethod;

            const { handle } = apiProvider('messenger');

            class TestAPI {
                @handle.availableIfVersion(isGreaterThanOrEqual(20, 4, 0))
                testMethod: () => void;
            }

            const testAPI = new TestAPI() as WithMeta<TestAPI>;

            testAPI.testMethod();

            expect(testMethod).toBeCalledTimes(0);
            expect(testAPI.testMethod.notSupported).toBeTruthy();
            expect(typeof testAPI.testMethod).toBe('function');
        });
    });
});
