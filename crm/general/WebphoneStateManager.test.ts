import {
  WebphoneNewIncomingCallEvent,
  WebphoneNewOutgoingCallEvent,
  WebphoneEstablishedCallEvent,
  WebphoneCallEndEvent,
  WebphoneOutgoingEventKind,
  WebphoneWidget,
  CallKind,
  WebphoneCallEndResolution,
} from '@yandex-telephony/ya-calls-webphone-sdk';
import { EventTarget } from 'event-target-shim';
import { WebphoneStateManager, CallState } from './WebphoneStateManager';

class WebphoneMock extends EventTarget {
  onIncomingCall(callback) {
    this.addEventListener(WebphoneOutgoingEventKind.NewIncomingCall, callback);
  }

  onOutgoingCall(callback) {
    this.addEventListener(WebphoneOutgoingEventKind.NewOutgoingCall, callback);
  }

  onCallEstablished(callback) {
    this.addEventListener(WebphoneOutgoingEventKind.EstablishedCall, callback);
  }

  onCallEnd(callback) {
    this.addEventListener(WebphoneOutgoingEventKind.CallEnd, callback);
  }
}

describe('WebphoneStateManager', () => {
  let webphone: EventTarget;
  let webphoneStateManager: WebphoneStateManager;

  beforeEach(() => {
    webphone = new WebphoneMock();
    webphoneStateManager = new WebphoneStateManager(webphone as WebphoneWidget);
  });

  it('should has default call state', () => {
    expect(webphoneStateManager.state).toEqual({ isInit: false });
  });

  it('should listen init event', () => {
    webphone.dispatchEvent(new CustomEvent(WebphoneOutgoingEventKind.Initialized));
    expect(webphoneStateManager.state).toEqual({ isInit: true });
  });

  it('should listen incoming call event', () => {
    const callEvent: WebphoneNewIncomingCallEvent = {
      type: WebphoneOutgoingEventKind.NewIncomingCall,
      sessionId: 'sessionId',
      callId: 'callId',
      remoteNumber: 'remoteNumber',
    };

    webphone.dispatchEvent(callEvent);
    expect(webphoneStateManager.state.call).toEqual({
      id: 'callId',
      kind: CallKind.Incoming,
      state: CallState.Pending,
    });
  });

  it('should listen outgoing call event', () => {
    const callEvent: WebphoneNewOutgoingCallEvent = {
      type: WebphoneOutgoingEventKind.NewOutgoingCall,
      sessionId: 'sessionId',
      callId: 'callId',
      remoteNumber: 'remoteNumber',
    };

    webphone.dispatchEvent(callEvent);
    expect(webphoneStateManager.state.call).toEqual({
      id: 'callId',
      kind: CallKind.Outgoing,
      state: CallState.Pending,
    });
  });

  it('should listen call established event', () => {
    const callEvent: WebphoneEstablishedCallEvent = {
      type: WebphoneOutgoingEventKind.EstablishedCall,
      sessionId: 'sessionId',
      callId: 'callId',
      callKind: CallKind.Incoming,
      remoteNumber: 'remoteNumber',
    };

    webphone.dispatchEvent(callEvent);
    expect(webphoneStateManager.state.call).toEqual({
      id: 'callId',
      kind: CallKind.Incoming,
      state: CallState.Accepted,
    });
  });

  it('should listen call end event', () => {
    const callEstablishedEvent: WebphoneEstablishedCallEvent = {
      type: WebphoneOutgoingEventKind.EstablishedCall,
      sessionId: 'sessionId',
      callId: 'callId',
      callKind: CallKind.Incoming,
      remoteNumber: 'remoteNumber',
    };

    const callEndEvent: WebphoneCallEndEvent = {
      type: WebphoneOutgoingEventKind.CallEnd,
      sessionId: 'sessionId',
      callId: 'callId',
      resolution: WebphoneCallEndResolution.Completed,
    };

    expect(webphoneStateManager.state.call).toBeUndefined();
    webphone.dispatchEvent(callEstablishedEvent);
    expect(webphoneStateManager.state.call).not.toBeUndefined();
    webphone.dispatchEvent(callEndEvent);
    expect(webphoneStateManager.state.call).toBeUndefined();
  });

  describe('setCrmCallId', () => {
    it('should not create state without active call', () => {
      webphoneStateManager.setCrmCallId(100);
      expect(webphoneStateManager.state.call).toBeUndefined();
    });

    it('should add crm callId to active call state', () => {
      const callEstablishedEvent: WebphoneEstablishedCallEvent = {
        type: WebphoneOutgoingEventKind.EstablishedCall,
        sessionId: 'sessionId',
        callId: 'callId',
        callKind: CallKind.Incoming,
        remoteNumber: 'remoteNumber',
      };

      webphone.dispatchEvent(callEstablishedEvent);
      webphoneStateManager.setCrmCallId(100);
      expect(webphoneStateManager.state.call!.crmCallId).toBe(100);
    });
  });

  describe('setParentIssue', () => {
    it('does not create state without active call', () => {
      webphoneStateManager.setParentIssue(1, 2);
      expect(webphoneStateManager.state.issue).toBeUndefined();
    });

    it('adds issue to active call state', () => {
      const callEstablishedEvent: WebphoneEstablishedCallEvent = {
        type: WebphoneOutgoingEventKind.EstablishedCall,
        sessionId: 'sessionId',
        callId: 'callId',
        callKind: CallKind.Incoming,
        remoteNumber: 'remoteNumber',
      };

      webphone.dispatchEvent(callEstablishedEvent);
      webphoneStateManager.setParentIssue(1, 2);
      expect(webphoneStateManager.state.issue).toStrictEqual({ id: 1, typeId: 2 });
    });
  });
});
