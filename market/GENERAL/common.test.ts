import {
  BeforeMiddlewareInterface,
  BodyType,
  createFetchApi,
  createPipeline,
  RequestMethod,
  ResponseType,
} from '../main/resources/common';

const textData = 'some text data';
const jsonData = { data: 'some data' };
const blobData = new Blob();

const createResponse = (url: string, response: object = {}): Response =>
  ({
    url,
    ok: true,
    json: () => Promise.resolve(jsonData),
    text: () => Promise.resolve(textData),
    blob: () => Promise.resolve(blobData),
    clone: () => createResponse(url),
    ...response,
  } as Response);

const createCustomFetch = (response?: object) => {
  return jest.fn((url: string, options?: RequestInit) => {
    return Promise.resolve<Response>(createResponse(url, response));
  });
};

describe('createPipeline', () => {
  it('must be run sequence', () => {
    let count = 0;
    const initial: object = {};
    const start = createPipeline(
      [
        (result, next) => next({ ...result, a: count++ }),
        (result, next) => next({ ...result, b: count++ }),
        (result, next) => next({ ...result, c: count++ }),
      ],
      initial
    );

    expect(start()).toEqual({
      a: 0,
      b: 1,
      c: 2,
    });
  });

  it('must be run reversed', () => {
    let count = 0;
    const initial: object = {};
    const start = createPipeline(
      [
        (result, next) => {
          const last = next(result);

          return { ...last, a: count++ };
        },
        (result, next) => {
          const last = next(result);

          return { ...last, b: count++ };
        },
        (result, next) => {
          const last = next(result);

          return { ...last, c: count++ };
        },
      ],
      initial
    );

    expect(start()).toEqual({
      a: 2,
      b: 1,
      c: 0,
    });
  });
});

describe('createFetchApi', () => {
  it('request method', async () => {
    const createFetch = (method: RequestMethod) => {
      const beforeMiddleware: BeforeMiddlewareInterface = (request, next) => {
        expect(request.options.method).toBe(method);

        return next(request);
      };

      return createFetchApi({ customFetch: createCustomFetch() }).before(beforeMiddleware);
    };

    await createFetch(RequestMethod.GET)('http://localhost');
    await createFetch(RequestMethod.GET)('http://localhost', { method: RequestMethod.GET });
    await createFetch(RequestMethod.DELETE)('http://localhost', { method: RequestMethod.DELETE });
    await createFetch(RequestMethod.OPTIONS)('http://localhost', { method: RequestMethod.OPTIONS });
    await createFetch(RequestMethod.PATCH)('http://localhost', { method: RequestMethod.PATCH });
    await createFetch(RequestMethod.POST)('http://localhost', { method: RequestMethod.POST });
    await createFetch(RequestMethod.PUT)('http://localhost', { method: RequestMethod.PUT });
  });

  it('correct set query parameters', async () => {
    const customFetch = createCustomFetch();

    const beforeMiddleware1: BeforeMiddlewareInterface = (request, next) => {
      expect(request.url).toBe('http://localhost/api?q=1&foo=bar');

      return next(request);
    };
    const fetchApi1 = createFetchApi({ customFetch }).before(beforeMiddleware1);
    await fetchApi1('http://localhost/api?q=1', {
      query: {
        foo: 'bar',
        baz: undefined,
      },
    });

    const beforeMiddleware2: BeforeMiddlewareInterface = (request, next) => {
      expect(request.url).toBe('http://localhost/api?foo=bar');

      return next(request);
    };
    const fetchApi2 = createFetchApi({ customFetch }).before(beforeMiddleware2);
    await fetchApi2('http://localhost/api', {
      query: {
        foo: 'bar',
        baz: undefined,
      },
    });
  });

  it('request with error response', async () => {
    const customFetch = createCustomFetch({ ok: false });
    const fetchApi = createFetchApi({ customFetch });

    try {
      await fetchApi('');
    } catch (e) {
      expect(e.ok).toBeFalsy();
    }

    try {
      await fetchApi('', { body: {} });
    } catch (e) {
      expect(e.ok).toBeFalsy();
    }
  });

  it('request with responseType', async () => {
    const customFetch = createCustomFetch();
    const fetchApi = createFetchApi({ customFetch });

    expect(await fetchApi('')).toBe(textData);
    expect(await fetchApi('', { responseType: ResponseType.BLOB })).toBe(blobData);
    expect(await fetchApi('', { responseType: ResponseType.JSON })).toBe(jsonData);
    expect(await fetchApi('', { responseType: ResponseType.TEXT })).toBe(textData);
    expect(await fetchApi('', { responseType: ResponseType.VOID })).toBeUndefined();
  });

  it('request with bodyType', async () => {
    const customFetch = createCustomFetch();

    const bodyTypeJson = (body: string): BeforeMiddlewareInterface => (request, next) => {
      expect(request.requestOptions.bodyType).toBe(BodyType.JSON);
      expect(request.options.body).toBe(body);
      expect(request.options.headers!['Content-Type']).toBe('application/json');

      return next(request);
    };

    const bodyTypeForm: BeforeMiddlewareInterface = (request, next) => {
      expect(request.requestOptions.bodyType).toBe(BodyType.FORM);
      expect(request.options.body).toBeInstanceOf(FormData);

      return next(request);
    };

    await createFetchApi({ customFetch }).before(bodyTypeJson('{}'))('a', { bodyType: BodyType.JSON, body: {} });
    await createFetchApi({ customFetch }).before(bodyTypeJson('string body'))('b', {
      bodyType: BodyType.JSON,
      body: 'string body',
    });
    await createFetchApi({ customFetch }).before(bodyTypeForm)('c', {
      bodyType: BodyType.FORM,
      body: { key: 'value' },
    });
  });

  it('request with custom response', async () => {
    const customFetch = createCustomFetch();

    const fetchApi1 = createFetchApi({ customFetch }).after((response, next) =>
      next({
        ...response,
        result: 'foo',
      })
    );
    const fetchApi2 = createFetchApi({ customFetch }).after((response, next) =>
      next({
        ...response,
        result: Promise.resolve('foo'),
      })
    );

    expect(await fetchApi1('')).toBe('foo');
    expect(await fetchApi2('')).toBe('foo');
  });

  it('before and after must be function', () => {
    const customFetch = createCustomFetch();
    const fetchApi = createFetchApi({ customFetch });

    expect(typeof fetchApi.before).toBe('function');
    expect(typeof fetchApi.after).toBe('function');
  });
});
