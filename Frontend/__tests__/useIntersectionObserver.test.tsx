import { act } from 'react-test-renderer';
import { renderHook } from 'neo/tests/renderHook';
import { useIntersectionObserver } from 'neo/hooks/useIntersectionObserver';

const createIntersectionObserverMock = () => {
  const callbacks: IntersectionObserverCallback[] = [];
  const observe = jest.fn();
  const unobserve = jest.fn();
  const disconnect = jest.fn();

  const IntersectionObserver = jest.fn((cb: IntersectionObserverCallback) => {
    callbacks.push(cb);

    return {
      observe,
      unobserve,
      disconnect,
    };
  });

  return {
    IntersectionObserver,
    callbacks,
    observe,
    unobserve,
    disconnect,
  };
};

describe('useIntersectionObserver', () => {
  let IntersectionObserverMock: ReturnType<typeof createIntersectionObserverMock>;
  let uniqConfigCounter = 0;

  const getUniqConfig = () => {
    uniqConfigCounter += 1;

    return {
      root: null,
      rootMargin: '0px 0px 0px 0px',
      threshold: 0.1 * uniqConfigCounter,
    };
  };

  beforeEach(() => {
    IntersectionObserverMock = createIntersectionObserverMock();

    Object.defineProperty(window, 'IntersectionObserver', {
      writable: true,
      value: IntersectionObserverMock.IntersectionObserver,
    });
  });

  afterEach(() => {
    Object.defineProperty(window, 'IntersectionObserver', {
      writable: true,
      value: undefined,
    });
  });

  it('Должен отписать observer от элемента когда вызывается unobserve', () => {
    const { unobserve, callbacks } = IntersectionObserverMock;

    const elem = document.createElement('div');
    const elemRef = { current: elem };
    const getResult = renderHook(() => useIntersectionObserver(elemRef, getUniqConfig()));

    act(() => {
      getResult()[1]();
    });

    expect(window.IntersectionObserver).toBeCalledTimes(1);
    expect(unobserve).toBeCalledTimes(1);
    expect(getResult()[0]).toBe(false);

    act(() => {
      const entry = { target: elem, isIntersecting: true } as unknown as IntersectionObserverEntry;
      const observer = {} as IntersectionObserver;
      callbacks[0]([entry], observer);
    });

    expect(getResult()[0]).toBe(false);
  });

  it('Должен установить isIntersecting в true когда элемент находиться в зоне пересечения', () => {
    const { callbacks } = IntersectionObserverMock;

    const elem = document.createElement('div');
    const elemRef = { current: elem };
    const getResult = renderHook(() => useIntersectionObserver(elemRef, getUniqConfig()));

    // Вызываем колбеки эмулируя вызов IntersectionObserver
    act(() => {
      callbacks.forEach((cb) => {
        const entry = { target: elem, isIntersecting: true } as unknown as IntersectionObserverEntry;
        const observer = {} as IntersectionObserver;
        cb([entry], observer);
      });
    });

    expect(callbacks.length).toBe(1);
    expect(getResult()[0]).toBe(true);
  });

  it.todo('Должен отписать observer когда компонент размонтируется');

  it('Должен применить несколько observer к одному элементу', () => {
    const { callbacks } = IntersectionObserverMock;

    const elem = document.createElement('div');
    const elemRef = { current: elem };
    const getResult1 = renderHook(() => useIntersectionObserver(elemRef, getUniqConfig()));
    const getResult2 = renderHook(() => useIntersectionObserver(elemRef, getUniqConfig()));

    expect(callbacks.length).toBe(2);

    // Эмулируем вызов IntersectionObserver для одного конфига
    act(() => {
      const entry = { target: elem, isIntersecting: true } as unknown as IntersectionObserverEntry;
      const observer = {} as IntersectionObserver;

      callbacks[0]([entry], observer);
    });

    expect(getResult1()[0]).toBe(true);
    expect(getResult2()[0]).toBe(false);
  });
});
