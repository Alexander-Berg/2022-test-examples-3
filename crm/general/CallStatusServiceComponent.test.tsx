import React from 'react';
import { EventTarget } from 'event-target-shim';
import {
  WebphoneWidget,
  WebphoneOutgoingEventKind,
  CallKind,
} from '@yandex-telephony/ya-calls-webphone-sdk';
import { render } from '@testing-library/react';
import { mocked } from 'ts-jest/utils';
import { CallStatusServiceComponent } from './CallStatusServiceComponent';
import { CallStatusService } from '../../services/CallStatusService';

jest.mock('services/Logger');
jest.mock('../../services/CallStatusService');

const MockCallStatusService = mocked(CallStatusService, true);

const webphoneMock = new EventTarget();
const mockCallStatusService = MockCallStatusService.mock.instances[0];

describe('CallStatusServiceComponent', () => {
  beforeEach(() => {
    mocked(mockCallStatusService.startCall).mockClear();
    mocked(mockCallStatusService.endCall).mockClear();
  });

  it('should call startCall on NewOutgoingCall', () => {
    render(<CallStatusServiceComponent webphone={webphoneMock as WebphoneWidget} />);

    webphoneMock.dispatchEvent({ type: WebphoneOutgoingEventKind.NewOutgoingCall, callId: '1' });

    const mockCallStatusService = MockCallStatusService.mock.instances[0];

    expect(mockCallStatusService.startCall).toBeCalledTimes(1);
  });

  it('should not call startCall on NewIncomingCall', () => {
    render(<CallStatusServiceComponent webphone={webphoneMock as WebphoneWidget} />);

    webphoneMock.dispatchEvent({ type: WebphoneOutgoingEventKind.NewIncomingCall, callId: '1' });

    const mockCallStatusService = MockCallStatusService.mock.instances[0];

    expect(mockCallStatusService.startCall).toBeCalledTimes(0);
  });

  it('should call startCall on EstablishedCall with incoming call kind ', () => {
    render(<CallStatusServiceComponent webphone={webphoneMock as WebphoneWidget} />);

    webphoneMock.dispatchEvent({
      type: WebphoneOutgoingEventKind.EstablishedCall,
      callKind: CallKind.Incoming,
      callId: '1',
    });

    const mockCallStatusService = MockCallStatusService.mock.instances[0];

    expect(mockCallStatusService.startCall).toBeCalledTimes(1);
  });

  it('should not call startCall on EstablishedCall with outgoing call kind ', () => {
    render(<CallStatusServiceComponent webphone={webphoneMock as WebphoneWidget} />);

    webphoneMock.dispatchEvent({
      type: WebphoneOutgoingEventKind.EstablishedCall,
      callKind: CallKind.Outgoing,
      callId: '1',
    });

    const mockCallStatusService = MockCallStatusService.mock.instances[0];

    expect(mockCallStatusService.startCall).toBeCalledTimes(0);
  });

  it('should not call endCall on CallEnd without initial event', () => {
    render(<CallStatusServiceComponent webphone={webphoneMock as WebphoneWidget} />);

    webphoneMock.dispatchEvent({ type: WebphoneOutgoingEventKind.CallEnd, callId: '1' });

    const mockCallStatusService = MockCallStatusService.mock.instances[0];

    expect(mockCallStatusService.endCall).toBeCalledTimes(0);
  });

  it('should call endCall for outgoing call', () => {
    render(<CallStatusServiceComponent webphone={webphoneMock as WebphoneWidget} />);

    webphoneMock.dispatchEvent({ type: WebphoneOutgoingEventKind.NewOutgoingCall, callId: '1' });

    webphoneMock.dispatchEvent({ type: WebphoneOutgoingEventKind.CallEnd, callId: '1' });

    const mockCallStatusService = MockCallStatusService.mock.instances[0];

    expect(mockCallStatusService.endCall).toBeCalledTimes(1);
  });

  it('should call endCall for incoming call', () => {
    render(<CallStatusServiceComponent webphone={webphoneMock as WebphoneWidget} />);

    webphoneMock.dispatchEvent({
      type: WebphoneOutgoingEventKind.EstablishedCall,
      callKind: CallKind.Incoming,
      callId: '1',
    });

    webphoneMock.dispatchEvent({ type: WebphoneOutgoingEventKind.CallEnd, callId: '1' });

    const mockCallStatusService = MockCallStatusService.mock.instances[0];

    expect(mockCallStatusService.endCall).toBeCalledTimes(1);
  });
});
