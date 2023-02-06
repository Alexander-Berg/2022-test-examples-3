import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { ETypeString } from 'types/entities';
import { Config } from 'types/Config';
import { terminalIdService } from 'services/TerminalIdService';
import { notifications } from 'services/Notifications';
import { mocked } from 'ts-jest/utils';
import { config } from 'services/Config';
import { CallService } from './CallService';
import { CallOptions } from './CallService.types';
import { errors } from './CallService.config';

jest.mock('services/Notifications', () => ({
  notifications: {
    warning: jest.fn(),
  },
}));

const mockedNotifications = mocked(notifications);

const requestDataSpy = jest.fn();

const server = setupServer(
  rest.post('/yacalls/createCall', (req, res, ctx) => {
    requestDataSpy(req.body);
    return res(ctx.status(200));
  }),
);

const callService = new CallService();

beforeAll(() => {
  server.listen();
});
afterAll(() => {
  server.close();
});

beforeEach(() => {
  requestDataSpy.mockClear();
  mockedNotifications.warning.mockClear();
});

describe('CallService', () => {
  const options: CallOptions = { kikId: 1, source: { eid: 2, etype: ETypeString.Issue } };

  describe('.call', () => {
    describe('double call same time', () => {
      beforeEach(() => {
        config.value = { useExternalPhone: true } as Config;
      });

      it('waits response from fist call', async () => {
        const call1 = callService.call(options);
        const call2 = callService.call(options);

        const [result1, result2] = await Promise.all([call1, call2]);

        const result3 = await callService.call(options);

        expect(result1).toBe('');
        expect(result2).toBe(false);
        expect(result3).toBe('');
      });

      it('sends warning notification', async () => {
        const call1 = callService.call(options);
        const call2 = callService.call(options);

        await Promise.all([call1, call2]);

        expect(mockedNotifications.warning).toBeCalledTimes(1);
        expect(mockedNotifications.warning).toBeCalledWith(errors.creatingCall);
      });
    });

    describe('when useExternalPhone === true', () => {
      beforeEach(() => {
        config.value = { useExternalPhone: true } as Config;
      });

      it('creates call with correct data', async () => {
        const result = await callService.call(options);

        expect(requestDataSpy.mock.calls[0][0]).toStrictEqual(options);
        expect(result).toBe('');
      });
    });

    describe('when useExternalPhone === false', () => {
      beforeEach(() => {
        config.value = { useExternalPhone: false } as Config;
      });

      describe('and has no terminalId', () => {
        it('sends warning notification', async () => {
          await callService.call(options);

          expect(mockedNotifications.warning).toBeCalledTimes(1);
          expect(mockedNotifications.warning).toBeCalledWith(errors.webphoneNotReady);
        });
      });

      describe('and has terminalId', () => {
        it('creates call with correct data', async () => {
          const terminalId = 'terminalId';
          terminalIdService.value = terminalId;

          const result = await callService.call(options);

          expect(requestDataSpy.mock.calls[0][0]).toStrictEqual({ ...options, terminalId });
          expect(result).toBe('');
        });
      });
    });
  });
});
