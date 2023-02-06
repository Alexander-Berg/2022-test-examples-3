import { createMemoryHistory } from 'history';
import { IssueCallNewNotifyData } from 'types/xiva/IssueCallNewNotify';
import { PinConfig } from 'types/PinConfig';
import { ETypeString } from 'types/entities';
import { CRMXiva, XivaBackendEventType } from 'modules/xiva';
import { TabMutex } from 'utils/TabMutex';
import { mocked } from 'ts-jest/utils';
import createLink from 'modules/issues/utils/createLink';
import { terminalIdService } from 'services/TerminalIdService';
import { activeCallIssueService } from 'services/ActiveCallIssueService';
import { showPinModalIfNeeded } from 'modules/pinForm';
import browserWindow from 'utils/browserWindow';
import { CallIssueRedirectByXiva } from '../CallIssueRedirectByXiva';

jest.mock('utils/browserWindow');
jest.mock('modules/issues/utils/createLink');
jest.mock('utils/TabMutex');
jest.mock('modules/pinForm');
jest.mock('services/ActiveCallIssueService');

const createLinkMockResult = 'createLink';

const browserWindowMock = mocked(browserWindow);
const TabMutexMock = mocked(TabMutex);
const createLinkMock = mocked(createLink);
const showPinModalIfNeededMock = mocked(showPinModalIfNeeded);

createLinkMock.mockImplementation(() => createLinkMockResult);
browserWindowMock.open.mockImplementation(() => ({}));

const xiva = new EventTarget() as CRMXiva;
const history = createMemoryHistory();

const historyPushSpy = jest.spyOn(history, 'push');

const issueForOpen = { id: 1, typeId: 2 };

const createOpenIssueEvent = (options?: Partial<IssueCallNewNotifyData>) => {
  const data: IssueCallNewNotifyData = { issue: issueForOpen, ...options };

  return new CustomEvent(XivaBackendEventType.IssueCallNewNotify, {
    detail: data,
  });
};

const pinConfig: PinConfig = {
  pinTarget: { etype: ETypeString.Issue, eid: 1 },
};

terminalIdService.value = 'terminalId';

describe('CallIssueRedirectByXiva', () => {
  beforeEach(() => {
    historyPushSpy.mockClear();
    createLinkMock.mockClear();
    showPinModalIfNeededMock.mockClear();
    TabMutexMock.createAndRun.mockClear();
    browserWindowMock.open.mockClear();
  });

  describe('when event has terminalId', () => {
    describe('and terminalId is for this tab', () => {
      it('opens issue in this tab', () => {
        const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);

        xiva.dispatchEvent(createOpenIssueEvent({ terminalId: terminalIdService.value }));
        callIssueRedirectByXiva.destroy();

        expect(createLinkMock).toBeCalledTimes(1);
        expect(createLinkMock).toBeCalledWith({ ...issueForOpen, hash: false });

        expect(historyPushSpy).toBeCalledTimes(1);
        expect(historyPushSpy).toBeCalledWith(createLinkMockResult);
      });

      it('sets issue for active call service', () => {
        const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);

        xiva.dispatchEvent(createOpenIssueEvent({ terminalId: terminalIdService.value }));
        callIssueRedirectByXiva.destroy();

        expect(activeCallIssueService.issue).toBe(issueForOpen);
      });

      describe('if has pin form config', () => {
        it('opens pin form', () => {
          const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);

          xiva.dispatchEvent(
            createOpenIssueEvent({
              terminalId: terminalIdService.value,
              autoShowPinFormConfig: pinConfig,
            }),
          );
          callIssueRedirectByXiva.destroy();

          expect(showPinModalIfNeededMock).toBeCalledTimes(1);
          expect(showPinModalIfNeededMock).toBeCalledWith(pinConfig);
        });
      });

      describe('if has no pin form config', () => {
        it('does not open pin form', () => {
          const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);

          xiva.dispatchEvent(
            createOpenIssueEvent({
              terminalId: terminalIdService.value,
            }),
          );
          callIssueRedirectByXiva.destroy();

          expect(showPinModalIfNeededMock).toBeCalledTimes(0);
        });
      });
    });

    describe('and terminalId is not for this tab', () => {
      it('does not open issue', () => {
        const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);

        xiva.dispatchEvent(
          createOpenIssueEvent({
            terminalId: 'terminalId_2',
          }),
        );
        callIssueRedirectByXiva.destroy();

        expect(createLinkMock).toBeCalledTimes(0);
        expect(historyPushSpy).toBeCalledTimes(0);
        expect(showPinModalIfNeededMock).toBeCalledTimes(0);
      });
    });
  });

  describe('when event has no terminalId', () => {
    it('opens issue in new tab', () => {
      const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);

      xiva.dispatchEvent(createOpenIssueEvent());
      callIssueRedirectByXiva.destroy();

      expect(TabMutexMock.createAndRun).toBeCalledTimes(1);
      TabMutexMock.createAndRun.mock.calls[0][0].task();

      expect(createLinkMock).toBeCalledTimes(1);
      expect(createLinkMock).toBeCalledWith({ ...issueForOpen, hash: false });

      expect(browserWindowMock.open).toBeCalledTimes(1);
      expect(browserWindowMock.open).toBeCalledWith(createLinkMockResult);
    });

    describe('if has pin form config', () => {
      it('sets pinConfig for child window', () => {
        const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);

        xiva.dispatchEvent(createOpenIssueEvent({ autoShowPinFormConfig: pinConfig }));
        callIssueRedirectByXiva.destroy();
        TabMutexMock.createAndRun.mock.calls[0][0].task();

        const childWindow = browserWindowMock.open.mock.results[0].value;
        expect(childWindow.pinConfig).toBe(pinConfig);
      });
    });

    describe('if has no pin form config', () => {
      it('does not set pinConfig for child window', () => {
        const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);

        xiva.dispatchEvent(createOpenIssueEvent());
        callIssueRedirectByXiva.destroy();
        TabMutexMock.createAndRun.mock.calls[0][0].task();

        const childWindow = browserWindowMock.open.mock.results[0].value;
        expect(childWindow.pinConfig).toBeUndefined();
      });
    });
  });

  describe('.destroy', () => {
    it('does not open issue', () => {
      const callIssueRedirectByXiva = new CallIssueRedirectByXiva(history, xiva);
      callIssueRedirectByXiva.destroy();

      xiva.dispatchEvent(createOpenIssueEvent({ terminalId: terminalIdService.value }));
      callIssueRedirectByXiva.destroy();

      expect(historyPushSpy).not.toBeCalled();
    });
  });
});
