import { EventTarget } from 'event-target-shim';
import {
  WebphoneOutgoingEventKind,
  WebphoneWidget,
  CallKind,
  WebphoneCallEndResolution,
} from '@yandex-telephony/ya-calls-webphone-sdk';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { act } from '@testing-library/react';
import 'appHistory';
import { delay } from 'utils/delay';
import { WebphoneIncomingCallService } from './WebphoneIncomingCallService';

const acceptCallResponse = { id: 100, typeId: 1, callId: 2 };

const server = setupServer(
  rest.post('/yacalls/acceptCall', (req, res, ctx) => {
    return res(ctx.json(acceptCallResponse));
  }),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const webphoneMock = new EventTarget();

describe('WebphoneIncomingCallService', () => {
  describe('on incoming call', () => {
    const webphoneIncomingCallService = new WebphoneIncomingCallService();
    beforeEach(() => {
      webphoneIncomingCallService.setWebphone(webphoneMock as WebphoneWidget);
    });

    it('calls onCallAccept callback', async () => {
      const callback = jest.fn();
      webphoneIncomingCallService.onCallAccept(callback);

      await act(async () => {
        webphoneMock.dispatchEvent({
          type: WebphoneOutgoingEventKind.EstablishedCall,
          sessionId: 'sessionId',
          callId: 'callId',
          callKind: CallKind.Incoming,
          remoteNumber: 'remoteNumber',
        });
      });

      await delay(0);

      expect(callback).toBeCalledWith({
        issueId: 100,
        issueTypeId: 1,
        callId: 2,
      });
    });
  });

  describe('on outgoing call', () => {
    const webphoneIncomingCallService = new WebphoneIncomingCallService();
    beforeEach(() => {
      webphoneIncomingCallService.setWebphone(webphoneMock as WebphoneWidget);
    });

    it('does not call onCallAccept callback', async () => {
      const callback = jest.fn();
      webphoneIncomingCallService.onCallAccept(callback);

      await act(async () => {
        webphoneMock.dispatchEvent({
          type: WebphoneOutgoingEventKind.EstablishedCall,
          sessionId: 'sessionId',
          callId: 'callId',
          callKind: CallKind.Outgoing,
          remoteNumber: 'remoteNumber',
        });
      });

      await delay(0);

      expect(callback).not.toBeCalled();
    });
  });

  describe('when call ends', () => {
    it('calls onCallEnd callback only once', async () => {
      const webphoneIncomingCallService = new WebphoneIncomingCallService();
      webphoneIncomingCallService.setWebphone(webphoneMock as WebphoneWidget);

      const mockCallEndCallback = jest.fn();
      webphoneIncomingCallService.onCallEnd(mockCallEndCallback);

      await act(async () => {
        webphoneMock.dispatchEvent({
          type: WebphoneOutgoingEventKind.EstablishedCall,
          sessionId: 'sessionId',
          callId: 'callId',
          callKind: CallKind.Incoming,
          remoteNumber: 'remoteNumber',
        });
      });

      await delay(0);

      await act(async () => {
        webphoneMock.dispatchEvent({
          type: WebphoneOutgoingEventKind.CallEnd,
          sessionId: 'sessionId',
          callId: 'callId',
          resolution: WebphoneCallEndResolution.Completed,
        });
      });

      await delay(0);

      await act(async () => {
        webphoneMock.dispatchEvent({
          type: WebphoneOutgoingEventKind.CallEnd,
          sessionId: 'sessionId',
          callId: 'callId',
          resolution: WebphoneCallEndResolution.Completed,
        });
      });

      await delay(0);

      expect(mockCallEndCallback).toBeCalledTimes(1);
      expect(mockCallEndCallback).toBeCalledWith({
        ycCallId: 'callId',
        issueId: acceptCallResponse.id,
        callId: acceptCallResponse.callId,
      });
    });
  });
});
