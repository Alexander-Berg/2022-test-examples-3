import { fetchExtended } from './fetchExtended';

describe(`fetchExtended`, () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe(`if need to retry`, () => {
    it('retries the request N times', async () => {
      global.fetch = jest.fn().mockRejectedValue('error');
      try {
        await fetchExtended(`/someUrl`, { retryOptions: { count: 3 } });
      } catch (error) {
        expect(global.fetch).toBeCalledTimes(4);
      }
    });
  });
  describe(`if don't need to retry`, () => {
    it(`doesn't retry the request`, async () => {
      global.fetch = jest.fn().mockRejectedValue('error');
      try {
        await fetchExtended(`/someUrl`);
      } catch (error) {
        expect(global.fetch).toBeCalledTimes(1);
      }
    });
  });
  describe(`if sets timeout abort`, () => {
    it(`calls abort for the request`, async () => {
      global.fetch = jest
        .fn()
        .mockImplementation(() => new Promise((resolve) => setTimeout(() => resolve('test'), 500)));
      const abortSpy = jest.spyOn(AbortController.prototype, 'abort');
      await fetchExtended(`/someUrl`, { timeout: 200 });
      expect(abortSpy).toBeCalledTimes(1);
    });
  });
});
