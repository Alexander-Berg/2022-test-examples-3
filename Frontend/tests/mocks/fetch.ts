export const RESPONSE_NETWORK_ERROR = new TypeError('__networkError__');
export const RESPONSE_SERVER_ERROR = { ok: false, status: 500 };
export const jsonMock = {
  foo: 'bar',
};
export const RESPONSE_OK = {
  ok: true,
  status: 200,
  json: () => Promise.resolve(jsonMock),
};
export const RESPONSE_OK_TEXT = {
  ok: true,
  status: 200,
  text: () => Promise.resolve('__responseBody__'),
};

export function mockFetch<TResponse>(response: TResponse) {
  mockFetchCalls([response]);
}

export function mockFetchCalls<TResponse>(responses: TResponse[]) {
  window.fetch = responses.reduce((mock, response) => (
    mock.mockImplementationOnce(() => Promise.resolve(response))
  ), jest.fn());
}
