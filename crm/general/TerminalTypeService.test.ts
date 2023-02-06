import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { waitFor } from '@testing-library/react';
import { TerminalTypeService } from './TerminalTypeService';
import {
  TERMINAL_URL,
  RESET_PASSWORD_URL,
  TERMINAL_SOFTPHONE_URL,
} from './TerminalTypeService.constants';
import { sofftPhoneInfoStub, newPasswordStub, terminalTypeStub } from './TerminalTypeService.stub';

const server = setupServer(
  rest.get(TERMINAL_SOFTPHONE_URL, (req, res, ctx) => {
    return res(ctx.json(sofftPhoneInfoStub));
  }),
  rest.post(RESET_PASSWORD_URL, (req, res, ctx) => {
    return res(ctx.json(newPasswordStub));
  }),
  rest.post(TERMINAL_URL, (req, res, ctx) => {
    return res(ctx.status(200));
  }),
);

describe('TerminalTypeService', () => {
  beforeAll(() => server.listen());
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());

  describe('when fetching softPhone info', () => {
    it('loads softPhone info', async () => {
      const service = new TerminalTypeService();
      service.getSoftPhoneInfo();

      expect(service.isSoftPhoneInfoLoading).toBe(true);
      await waitFor(() => {
        expect(service.isSoftPhoneInfoLoading).toBe(false);
        expect(service.softPhoneInfo).toStrictEqual(sofftPhoneInfoStub);
      });
    });
  });

  describe('when fetching new password', () => {
    it('loads new password in sofftPhoneInfo', async () => {
      const service = new TerminalTypeService();
      service.getSoftPhoneInfo();
      service.resetSoftPhonePassword();

      expect(service.isSoftPhoneInfoLoading).toBe(true);
      await waitFor(() => {
        expect(service.isSoftPhoneInfoLoading).toBe(false);
        expect(service.softPhoneInfo?.password).toStrictEqual(newPasswordStub.password);
      });
    });
  });

  describe('when fetching new terminal type', () => {
    it('requests and loads new terminal type ', async () => {
      const confirm = (window.confirm = jest.fn(() => true));

      const service = new TerminalTypeService();
      service.setTerminalType(terminalTypeStub);

      expect(confirm).toBeCalled();
      expect(service.isTerminalTypeLoading).toBe(true);
      await waitFor(() => {
        expect(service.isTerminalTypeLoading).toBe(false);
      });
    });
  });
});
