jest.mock('../../../filters/contacts/aceventura-suggest.js');

const aceventuraSuggestReport = require('../aceventura-suggest');
const filter = require('../../../filters/contacts/aceventura-suggest.js');

describe('models:aceventura -> aceventura-suggest', () => {
  let coreMock;
  let serviceFn;
  let aceventuraFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    filter.mockClear();
    serviceFn = jest.fn();
    aceventuraFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid})
      },
      config: {
        connectionid,
        IS_CORP: false
      }
    };

    serviceFn.mockReturnValue(aceventuraFn);
  });

  test('должен вызывать сервис aceventura', async () => {
    const params = {
      q: '123',
      user_type: 'pdd'
    };

    await aceventuraSuggestReport(params, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('aceventura');
  });

  test('должен передавать все нужные парамерты в сервис aceventura', async () => {
    const params = {
      q: '123',
      user_type: 'pdd'
    };

    await aceventuraSuggestReport(params, coreMock);

    expect(aceventuraFn).toHaveBeenCalledTimes(1);
    expect(aceventuraFn).toHaveBeenCalledWith('/v1/suggest', {
      query: params.q,
      shared: 'include',
      search_session_id: connectionid,
      has_telephone_number: false,
      limit: 10,
      user_id: uid,
      user_type: 'passport_user'
    });
  });

  test('должен возвращать ответ от aceventura, пропущенный через фильтр', async () => {
    const params = {
      q: '123',
      user_type: 'pdd'
    };
    const response = Symbol();
    const filterResult = Symbol();

    filter.mockReturnValue(filterResult);
    aceventuraFn.mockReturnValue(response);

    const modelResult = await aceventuraSuggestReport(params, coreMock);

    expect(filter).toHaveBeenCalledTimes(1);
    expect(filter).toHaveBeenCalledWith(response, false);
    expect(modelResult).toBe(filterResult);
  });
});
