import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { cleanup } from '@testing-library/react/pure';
import { CRMXiva, XivaBackendEventType } from 'modules/xiva';
import { XivaContext } from 'modules/xiva';
import { CallStatus } from 'modules/xiva/types/PersonalCall';
import { EType } from 'types/entities';
import { PersonalCallServiceStub, Provider } from 'services/PersonalCallService';
import { CallPopupContainer } from './CallPopupContainer';

const xiva = new EventTarget() as CRMXiva;

const createCallEvent = (id: number, status: CallStatus) => {
  return new CustomEvent(XivaBackendEventType.Activities, {
    detail: { id, type: EType.YcCall, status },
  });
};

const personalCallServiceStub = new PersonalCallServiceStub();

personalCallServiceStub.loadCallData = jest.fn();
personalCallServiceStub.reloadCallData = jest.fn();
personalCallServiceStub.getLastNotDoneActivity = jest.fn();

describe('CallPopupContainer', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('when call starts', () => {
    it('renders popup in active state', async () => {
      render(
        <TestBed>
          <XivaContext.Provider value={xiva}>
            <Provider value={personalCallServiceStub}>
              <CallPopupContainer />
            </Provider>
          </XivaContext.Provider>
        </TestBed>,
      );

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callOperatorAnswered'));
      });

      const popup = screen.getByTestId('call-popup');
      const popupIcon = screen.getByTestId('call-popup-icon');

      expect(popup).toBeVisible();
      expect(popup).toHaveClass('CallPopup_isCallActive');
      expect(popupIcon).toBeVisible();
      expect(popupIcon).toHaveClass('CallPopupIcon_isCallActive');

      await waitFor(() => {
        expect(personalCallServiceStub.loadCallData).toBeCalledTimes(1);
        expect(personalCallServiceStub.loadCallData).toBeCalledWith(1);
      });

      cleanup();
    });

    it('works correctly for several starts', async () => {
      render(
        <TestBed>
          <XivaContext.Provider value={xiva}>
            <Provider value={personalCallServiceStub}>
              <CallPopupContainer />
            </Provider>
          </XivaContext.Provider>
        </TestBed>,
      );

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callOperatorAnswered'));
      });

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callOperatorAnswered'));
      });

      act(() => {
        xiva.dispatchEvent(createCallEvent(1, 'callOperatorAnswered'));
      });

      const popup = screen.getByTestId('call-popup');
      const popupIcon = screen.getByTestId('call-popup-icon');

      expect(popup).toBeVisible();
      expect(popup).toHaveClass('CallPopup_isCallActive');
      expect(popupIcon).toBeVisible();
      expect(popupIcon).toHaveClass('CallPopupIcon_isCallActive');

      await waitFor(() => {
        expect(personalCallServiceStub.loadCallData).toBeCalledTimes(1);
        expect(personalCallServiceStub.loadCallData).toBeCalledWith(1);
      });

      cleanup();
    });
  });

  describe('when call ends', () => {
    it('renders popup in ended call style', async () => {
      render(
        <TestBed>
          <XivaContext.Provider value={xiva}>
            <Provider value={personalCallServiceStub}>
              <CallPopupContainer />
            </Provider>
          </XivaContext.Provider>
        </TestBed>,
      );

      act(() => {
        xiva.dispatchEvent(createCallEvent(2, 'callOperatorAnswered'));
      });

      act(() => {
        xiva.dispatchEvent(createCallEvent(2, 'callFinished'));
      });

      const popup = screen.getByTestId('call-popup');
      const popupIcon = screen.getByTestId('call-popup-icon');

      expect(popup).toBeVisible();
      expect(popup).not.toHaveClass('CallPopup_isCallActive');
      expect(popupIcon).toBeVisible();
      expect(popupIcon).not.toHaveClass('CallPopupIcon_isCallActive');

      await waitFor(() => {
        expect(personalCallServiceStub.loadCallData).toBeCalledTimes(1);
        expect(personalCallServiceStub.loadCallData).toBeCalledWith(2);
      });
    });

    describe('when callId is wrong', () => {
      it('does nothing', async () => {
        render(
          <TestBed>
            <XivaContext.Provider value={xiva}>
              <Provider value={personalCallServiceStub}>
                <CallPopupContainer />
              </Provider>
            </XivaContext.Provider>
          </TestBed>,
        );

        act(() => {
          xiva.dispatchEvent(createCallEvent(4, 'callOperatorAnswered'));
        });

        act(() => {
          xiva.dispatchEvent(createCallEvent(5, 'callFinished'));
        });

        const popup = screen.getByTestId('call-popup');
        const popupIcon = screen.getByTestId('call-popup-icon');

        expect(popup).toBeVisible();
        expect(popup).toHaveClass('CallPopup_isCallActive');
        expect(popupIcon).toBeVisible();
        expect(popupIcon).toHaveClass('CallPopupIcon_isCallActive');

        await waitFor(() => {
          expect(personalCallServiceStub.loadCallData).toBeCalledTimes(1);
          expect(personalCallServiceStub.loadCallData).toBeCalledWith(4);
        });
      });
    });
  });

  describe('when call updates', () => {
    it('reloads call data', async () => {
      render(
        <TestBed>
          <XivaContext.Provider value={xiva}>
            <Provider value={personalCallServiceStub}>
              <CallPopupContainer />
            </Provider>
          </XivaContext.Provider>
        </TestBed>,
      );

      act(() => {
        xiva.dispatchEvent(createCallEvent(3, 'callOperatorAnswered'));
      });

      act(() => {
        xiva.dispatchEvent(createCallEvent(3, 'update'));
      });

      await waitFor(() => {
        expect(personalCallServiceStub.loadCallData).toBeCalledTimes(1);
        expect(personalCallServiceStub.reloadCallData).toBeCalledTimes(1);
        expect(personalCallServiceStub.reloadCallData).toBeCalledWith(3);
      });
    });
  });

  describe('when first mounts', () => {
    it('loads last not done activities', async () => {
      render(
        <TestBed>
          <XivaContext.Provider value={xiva}>
            <Provider value={personalCallServiceStub}>
              <CallPopupContainer />
            </Provider>
          </XivaContext.Provider>
        </TestBed>,
      );

      await waitFor(() => {
        expect(personalCallServiceStub.getLastNotDoneActivity).toBeCalledTimes(1);
      });
    });
  });
});
