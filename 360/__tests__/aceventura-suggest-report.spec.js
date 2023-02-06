const aceventuraSuggestReport = require('../aceventura-suggest-report');

describe('models:aceventura -> aceventura-suggest-report', () => {
  let coreMock;
  let serviceFn;
  let aceventuraFn;
  const uid = 1234567890;
  const connectionid = 'MAYA-1234567890';

  beforeEach(() => {
    serviceFn = jest.fn();
    aceventuraFn = jest.fn();
    coreMock = {
      service: serviceFn,
      auth: {
        get: () => ({uid})
      },
      config: {
        connectionid
      }
    };

    serviceFn.mockReturnValue(aceventuraFn);
  });

  test('должен вызывать сервис aceventura', () => {
    const params = {
      q: '123',
      contact_id: 123,
      email: 'pew@ya.ru',
      user_type: 'pdd'
    };

    aceventuraSuggestReport(params, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('aceventura');
  });

  test('должен передавать все нужные парамерты в сервис aceventura', () => {
    const params = {
      q: '123',
      contact_id: 123,
      email: 'pew@ya.ru',
      user_type: 'pdd'
    };

    aceventuraSuggestReport(params, coreMock);

    expect(aceventuraFn).toHaveBeenCalledTimes(1);
    expect(aceventuraFn).toHaveBeenCalledWith('/v1/suggestReport', {
      q: params.q,
      contact_id: params.contact_id,
      title: params.email,
      search_session_id: connectionid,
      user_id: uid,
      user_type: 'passport_user'
    });
  });

  test('должен возвращать ответ от aceventura', () => {
    const params = {
      q: '123',
      contact_id: 123,
      email: 'pew@ya.ru',
      user_type: 'pdd'
    };
    const response = Symbol();

    aceventuraFn.mockReturnValue(response);

    const modelResult = aceventuraSuggestReport(params, coreMock);

    expect(modelResult).toBe(response);
  });
});
