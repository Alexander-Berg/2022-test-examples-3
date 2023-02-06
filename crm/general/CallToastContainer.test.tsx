import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { CRMXiva, XivaBackendEventType } from 'modules/xiva';
import { XivaContext } from 'modules/xiva';
import { CallStatus } from 'modules/xiva/types/PersonalCall';
import { EType } from 'types/entities';
import { PersonalCallServiceStub, Provider } from '../../services/PersonalCallService';
import { CallToastContainer } from './CallToastContainer';

const xiva = new EventTarget() as CRMXiva;

const createCallEvent = (id: number, status: CallStatus) => {
  return new CustomEvent(XivaBackendEventType.Activities, {
    detail: { id, type: EType.YcCall, status },
  });
};

const personalCallServiceStub = new PersonalCallServiceStub();

personalCallServiceStub.loadCallData = jest.fn();

describe('CallToastContainer', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('when dial starts', () => {
    it('renders toast', async () => {
      render(
        <TestBed>
          <XivaContext.Provider value={xiva}>
            <Provider value={personalCallServiceStub}>
              <CallToastContainer />
            </Provider>
          </XivaContext.Provider>
        </TestBed>,
      );

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callStarted'));
      });

      expect(screen.getByTestId('call-toast')).toBeVisible();

      await waitFor(() => {
        expect(personalCallServiceStub.loadCallData).toBeCalledTimes(1);
        expect(personalCallServiceStub.loadCallData).toBeCalledWith(1);
      });
    });
  });

  describe('when operator answers', () => {
    it('hides toast', async () => {
      render(
        <TestBed>
          <XivaContext.Provider value={xiva}>
            <Provider value={personalCallServiceStub}>
              <CallToastContainer />
            </Provider>
          </XivaContext.Provider>
        </TestBed>,
      );

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callStarted'));
      });

      expect(screen.getByTestId('call-toast')).toBeVisible();

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callOperatorAnswered'));
      });

      expect(screen.queryByTestId('call-toast')).not.toBeInTheDocument();
    });
  });

  describe('when operator rejects', () => {
    it('hides toast', async () => {
      render(
        <TestBed>
          <XivaContext.Provider value={xiva}>
            <Provider value={personalCallServiceStub}>
              <CallToastContainer />
            </Provider>
          </XivaContext.Provider>
        </TestBed>,
      );

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callStarted'));
      });

      expect(screen.getByTestId('call-toast')).toBeVisible();

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callOperatorRejected'));
      });

      expect(screen.queryByTestId('call-toast')).not.toBeInTheDocument();
    });
  });
});
