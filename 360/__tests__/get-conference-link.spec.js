const getConferenceLink = require('../get-conference-link');

describe('models:telemost -> get-conference-link', () => {
  let coreMock;
  let serviceFn;
  let telemostFn;

  beforeEach(() => {
    serviceFn = jest.fn();
    telemostFn = jest.fn();
    coreMock = {
      service: serviceFn
    };

    serviceFn.mockReturnValue(telemostFn);
  });

  test('должен вызывать сервис telemost', () => {
    telemostFn.mockResolvedValue('');

    getConferenceLink({allowExternal: true}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('telemost');
  });

  test('должен ходить в ручку телемоста с нужными параметрами', () => {
    telemostFn.mockResolvedValue('');

    getConferenceLink({allowExternal: true}, coreMock);

    expect(telemostFn).toHaveBeenCalledTimes(1);
    expect(telemostFn).toHaveBeenCalledWith('/v1/telemost/conferences', {
      staff_only: false
    });
  });

  test('должен возвращать только uri', async () => {
    const uri = Symbol();
    const telemostResponse = {
      uri,
      someOtherMaybeSecretData: 'hide me!'
    };
    const expectedClientResponse = {uri};

    telemostFn.mockResolvedValue(telemostResponse);

    expect(await getConferenceLink({}, coreMock)).toEqual(expectedClientResponse);
  });
});
