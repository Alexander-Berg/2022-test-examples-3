import { skipCallWithSameArgs } from '../skipCallWithSameArgs';

const mockCallback = jest.fn((_value1?, _value2?) => {});

describe('skipCallWithSameArgs', () => {
  beforeEach(() => {
    mockCallback.mockClear();
  });

  describe('same args', () => {
    it('calls with empty args', () => {
      const enhancedMockCallback = skipCallWithSameArgs(mockCallback);
      enhancedMockCallback();
      enhancedMockCallback();

      expect(mockCallback).toBeCalledTimes(1);
    });

    it('calls with non empty args', () => {
      const enhancedMockCallback = skipCallWithSameArgs(mockCallback);
      enhancedMockCallback(1, 2);
      enhancedMockCallback(1, 2);

      expect(mockCallback).toBeCalledTimes(1);
    });
  });

  describe('different args', () => {
    it('calls with empty args first', () => {
      const enhancedMockCallback = skipCallWithSameArgs(mockCallback);
      enhancedMockCallback();
      enhancedMockCallback(1, 2);

      expect(mockCallback).toBeCalledTimes(2);
    });

    it('calls with non empty args first', () => {
      const enhancedMockCallback = skipCallWithSameArgs(mockCallback);
      enhancedMockCallback(1, 2);
      enhancedMockCallback();

      expect(mockCallback).toBeCalledTimes(2);
    });
  });
});
