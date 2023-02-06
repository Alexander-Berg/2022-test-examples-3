import { runIfTabVisibleOnce } from '../runIfTabVisibleOnce';

const setVisibilityState = (value: 'hidden' | 'visible') => {
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: function() {
      return value;
    },
  });
};

describe('runIfTabVisibleOnce', () => {
  const mockCallback = jest.fn();

  beforeEach(() => {
    mockCallback.mockClear();
  });

  it('does not run callback with visibilityState hidden', () => {
    setVisibilityState('hidden');

    const unsubscribe = runIfTabVisibleOnce(mockCallback);
    unsubscribe();

    expect(mockCallback).toBeCalledTimes(0);
  });

  it('runs callback with visibilityState visible', () => {
    setVisibilityState('visible');

    runIfTabVisibleOnce(mockCallback);

    expect(mockCallback).toBeCalledTimes(1);
  });

  it('runs callback on visibilitychange', () => {
    setVisibilityState('hidden');

    runIfTabVisibleOnce(mockCallback);

    setVisibilityState('visible');

    document.dispatchEvent(new Event('visibilitychange'));

    expect(mockCallback).toBeCalledTimes(1);
  });

  it('runs callback on visibilitychange only once', () => {
    setVisibilityState('hidden');

    runIfTabVisibleOnce(mockCallback);

    setVisibilityState('visible');

    document.dispatchEvent(new Event('visibilitychange'));
    document.dispatchEvent(new Event('visibilitychange'));

    expect(mockCallback).toBeCalledTimes(1);
  });

  it('supports unsubscribe', () => {
    setVisibilityState('hidden');

    const unsubscribe = runIfTabVisibleOnce(mockCallback);
    unsubscribe();

    setVisibilityState('visible');

    document.dispatchEvent(new Event('visibilitychange'));

    expect(mockCallback).toBeCalledTimes(0);
  });
});
