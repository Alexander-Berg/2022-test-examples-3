const duffman = require('@yandex-int/duffman');

const telemost = require('../telemost');
const TelemostError = require('../../errors/TelemostError');
const {TELEMOST_ERROR} = require('../../shared/errors/codes');

const CustomError = duffman.errors.CUSTOM_ERROR;

describe('services/telemost', () => {
  let coreMock;
  const userTicket = Symbol();
  const telemostSeviceTicket = Symbol();
  const telemostSeviceTicketMimino = Symbol();
  const isDev = jest.fn();
  const isTesting = jest.fn();
  const got = jest.fn();
  const consoleError = jest.fn();
  const method = '/some_method';

  beforeEach(() => {
    isDev.mockReturnValue(false);
    isTesting.mockReturnValue(false);
    got.mockReset();
    got.mockResolvedValue('');
    coreMock = {
      auth: {
        get: () => ({userTicket})
      },
      console: {
        error: consoleError
      },
      config: {
        services: {
          telemost: 'http://telemost'
        },
        isDev,
        isTesting
      },
      req: {
        tvm: {
          tickets: {
            mpfs: {
              ticket: telemostSeviceTicket
            },
            mpfs_mimino: {
              ticket: telemostSeviceTicketMimino
            }
          }
        }
      },
      got
    };
  });

  test('должен вызывать core.got с правильными параметрами', async () => {
    const expectedOptions = {
      json: true,
      method: 'post',
      headers: {
        'X-Ya-Service-Ticket': telemostSeviceTicket,
        'X-Ya-User-Ticket': userTicket
      }
    };

    await telemost(coreMock, method);

    expect(coreMock.got).toHaveBeenCalledTimes(1);
    expect(coreMock.got).toHaveBeenCalledWith('http://telemost/some_method', expectedOptions);
  });

  test('должен возвращать результат core.got', async () => {
    const telemostResponse = Symbol();

    coreMock.got.mockResolvedValue(telemostResponse);

    expect(await telemost(coreMock, method)).toBe(telemostResponse);
  });

  test('должен использовать нужные mpfs_mimino в dev', async () => {
    isDev.mockReturnValue(true);
    isTesting.mockReturnValue(true);

    await telemost(coreMock, method);
    expect(coreMock.got.mock.calls[0][1].headers['X-Ya-Service-Ticket']).toBe(
      telemostSeviceTicketMimino
    );
  });

  test('должен возвращать инстанс TelemostError при возникновении ошибки', async () => {
    const error = Symbol();

    coreMock.got.mockRejectedValue(new CustomError(error));

    try {
      await telemost(coreMock, method);
    } catch (error) {
      expect(error).toBeInstanceOf(TelemostError);
    }
  });

  test('должен писать в лог детали ошибки от Телемоста', async () => {
    const responseError = new CustomError(Symbol());

    coreMock.got.mockRejectedValue(responseError);
    consoleError.mockReset();

    try {
      await telemost(coreMock, method);
    } catch (error) {
      expect(consoleError).toHaveBeenCalledTimes(1);
      expect(consoleError).toHaveBeenCalledWith(TELEMOST_ERROR, responseError);
    }
  });
});
