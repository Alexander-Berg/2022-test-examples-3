import { ViewportObserver, observers } from '../ViewportObserver';

const viewportObserver = new ViewportObserver();

function createIntersectionObserverMock(
    { spyConstructor, spyObserve, spyUnobserve }: {
        spyConstructor?: () => void,
        spyObserve?: () => void,
        spyUnobserve?: () => void
    }
) {
    return class IntersectionObserverMock {
        callback
        constructor(cb) {
            this.callback = cb;
            if (spyConstructor) {
                spyConstructor();
            }
        }

        observe() {
            if (spyObserve) {
                spyObserve();
            }
        }

        unobserve() {
            if (spyUnobserve) {
                spyUnobserve();
            }
        }
    };
}

describe('ViewportObserver', () => {
    let spyConstructor: () => void;
    let spyUnobserve: () => void;
    let spyObserve: () => void;
    let spyCallbackTest: () => void;
    let spyCallbackTest2: () => void;
    let spyCallbackSuper: () => void;

    const elements = [
        document.createElement('div'),
        document.createElement('div'),
    ];

    beforeEach(() => {
        spyConstructor = jest.fn();
        spyUnobserve = jest.fn();
        spyObserve = jest.fn();
        spyCallbackTest = jest.fn();
        spyCallbackTest2 = jest.fn();
        spyCallbackSuper = jest.fn();

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (global as any).IntersectionObserver = createIntersectionObserverMock({
            spyConstructor,
            spyUnobserve,
            spyObserve,
        });
    });

    it('ViewportObserver - синглтон', () => {
        const newViewportObserver = new ViewportObserver();

        expect(newViewportObserver).toBe(viewportObserver);
    });

    it('Создается IntersectionObserver', () => {
        viewportObserver.create('test');

        expect(spyConstructor).toBeCalledTimes(1);
    });

    it('При создании инстанс IntersectionObserver сохраняется в словарь', () => {
        viewportObserver.create('test');

        expect(observers.test).toBeInstanceOf(IntersectionObserver);
    });

    it('Создается по одному IntersectionObserver для неймспейса', () => {
        viewportObserver.create('test');
        viewportObserver.create('super');

        expect(spyConstructor).toBeCalledTimes(2);
    });

    it('Вызываются методы observe и unobserve у IntersectionObserver', () => {
        viewportObserver.create('test');
        viewportObserver.observe('test', elements);

        expect(spyObserve).toBeCalledTimes(elements.length);

        viewportObserver.unobserve('test', [elements[0]]);

        expect(spyUnobserve).toBeCalledTimes(1);
    });

    it('При создании IntersectionObserver возвращается объект с рабочими методами', () => {
        const spySubscribe = jest.spyOn(viewportObserver, 'subscribe');
        const spyUnsubscribe = jest.spyOn(viewportObserver, 'unsubscribe');
        const callback = jest.fn();
        const tools = viewportObserver.create('test');

        tools.observe(elements);
        expect(spyObserve).toBeCalledTimes(elements.length);

        tools.unobserve(elements);
        expect(spyUnobserve).toBeCalledTimes(elements.length);

        tools.subscribe(callback);
        expect(spySubscribe).toBeCalledWith('test', callback);

        tools.unsubscribe(callback);
        expect(spyUnsubscribe).toBeCalledWith('test', callback);
    });

    it('При триггере вызывается callback с нужными аргументами', () => {
        const entries = [{} as IntersectionObserverEntry];

        viewportObserver.create('test');
        viewportObserver.create('super');
        viewportObserver.subscribe('test', spyCallbackTest);
        viewportObserver.subscribe('test', spyCallbackTest2);
        viewportObserver.subscribe('super', spyCallbackSuper);

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (observers.test as any).callback(entries, observers.test);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (observers.test as any).callback(entries, observers.test);

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (observers.super as any).callback(entries, observers.super);

        expect(spyCallbackTest).toBeCalledWith(entries, observers.test);
        expect(spyCallbackTest).toBeCalledTimes(2);

        expect(spyCallbackTest2).toBeCalledWith(entries, observers.test);
        expect(spyCallbackTest2).toBeCalledTimes(2);

        expect(spyCallbackSuper).toBeCalledWith(entries, observers.super);
        expect(spyCallbackSuper).toBeCalledTimes(1);
    });
});
