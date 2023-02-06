import { WebphoneEventManager } from 'modules/webphone/services/WebphoneEventManager';
import { mocked } from 'ts-jest/utils';
import { WebphoneWidget, WebphoneOutgoingEventKind } from '@yandex-telephony/ya-calls-webphone-sdk';
import { WebphoneService } from './WebphoneService';

jest.mock('modules/webphone/services/WebphoneEventManager', () => ({
  WebphoneEventManager: jest.fn(() => ({
    destroy: jest.fn(),
  })),
}));

const WebphoneEventManagerMocked = mocked(WebphoneEventManager);

describe('WebphoneService', () => {
  beforeEach(() => {
    WebphoneEventManagerMocked.mockClear();
  });

  describe('.constructor', () => {
    it('inits hasActiveCall with false', () => {
      const webphoneService = new WebphoneService();

      expect(webphoneService.hasActiveCall).toBe(false);
    });
  });

  describe('.setWebphone', () => {
    it('subscribes to webphone events', () => {
      const webphoneService = new WebphoneService();
      webphoneService.setWebphone({} as WebphoneWidget);
      webphoneService.destroy();

      expect(WebphoneEventManagerMocked).toBeCalled();
    });
  });

  describe('.destroy', () => {
    it('destroys WebphoneEventManager', () => {
      const webphoneService = new WebphoneService();
      webphoneService.setWebphone({} as WebphoneWidget);
      webphoneService.destroy();

      expect(WebphoneEventManagerMocked.mock.results[0].value.destroy).toBeCalled();
    });
  });

  describe('when call start', () => {
    it('sets hasActiveCall to true', () => {
      const webphoneService = new WebphoneService();
      webphoneService.setWebphone({} as WebphoneWidget);
      const events = WebphoneEventManagerMocked.mock.calls[0][1];
      events?.[WebphoneOutgoingEventKind.EstablishedCall]?.({} as never);

      webphoneService.destroy();

      expect(webphoneService.hasActiveCall).toBe(true);
    });
  });

  describe('when call end', () => {
    it('sets hasActiveCall to true', () => {
      const webphoneService = new WebphoneService();
      webphoneService.setWebphone({} as WebphoneWidget);
      const events = WebphoneEventManagerMocked.mock.calls[0][1];
      events?.[WebphoneOutgoingEventKind.EstablishedCall]?.({} as never);
      events?.[WebphoneOutgoingEventKind.CallEnd]?.({} as never);

      webphoneService.destroy();

      expect(webphoneService.hasActiveCall).toBe(false);
    });
  });
});
