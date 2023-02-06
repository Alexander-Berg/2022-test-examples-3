import { createApiMock } from './apiMock';

interface TestApi {
  someFn(x: number, s: string): Promise<boolean>;
}

describe('api-mock', () => {
  it('should resolve', async () => {
    const apiMock = createApiMock<TestApi>();
    const promise = apiMock.someFn(10, 'test');
    expect(apiMock.someFn.activeRequests()).toHaveLength(1);
    expect(apiMock.someFn.activeRequests()).toEqual([[10, 'test']]);
    apiMock.someFn
      .next((n, s) => {
        expect(n).toEqual(10);
        expect(s).toEqual('test');

        return true;
      })
      .resolve(true);

    const result = await promise;
    expect(result).toEqual(true);
  });

  it('should reject', async () => {
    const apiMock = createApiMock<TestApi>();
    const promise = apiMock.someFn(10, 'test');
    apiMock.someFn
      .next((n, s) => {
        expect(n).toEqual(10);
        expect(s).toEqual('test');

        return true;
      })
      .reject('ERROR');

    try {
      await promise;
      fail(`Shouldn't get here`);
    } catch (e) {
      expect(e).toEqual('ERROR');
    }
  });
});
