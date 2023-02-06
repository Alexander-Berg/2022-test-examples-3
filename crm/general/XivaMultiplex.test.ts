import { Subject, BehaviorSubject } from 'rxjs';
import { waitFor } from '@testing-library/dom';
import 'services/Logger';
import { UniqIdGenerator } from 'utils/UniqIdGenerator';
import { runPendingPromises } from 'utils/runPendingPromises';
import {
  XivaMessageAny,
  XivaErrorMessage,
  XivaDisconnectedMessage,
  XivaSubscribeMethodResponse,
} from 'types/xiva/XivaWebsocketJSONApi';
import { XivaMultiplex } from './XivaMultiplex';

jest.mock('utils/UniqIdGenerator');
jest.mock('services/Logger');

jest.useFakeTimers('modern');

const MOCKED_REQUEST_ID = '1';

UniqIdGenerator.global.next = () => MOCKED_REQUEST_ID;

const createTestEnv = () => {
  const websocket = new Subject<XivaMessageAny>();
  const sendMessageToServerMocked = jest.fn();

  const xivaCredits = {
    uid: 'uid',
    service: 'service',
    client: 'client',
    session: 'session',
  };

  const isWebsocketOpened = new BehaviorSubject(false);
  const getXivaCreditsMocked = jest.fn(() => Promise.resolve(xivaCredits));

  const sendMessageFromServer = websocket.next.bind(websocket) as typeof websocket.next;

  const multiplex = XivaMultiplex.create({
    messageFromServer: websocket,
    messageToServer: sendMessageToServerMocked,
    isWebsocketOpened,
    getXivaCredits: getXivaCreditsMocked,
  });

  return {
    multiplex,
    sendMessageToServerMocked,
    sendMessageFromServer,
    websocket,
    isWebsocketOpened,
    getXivaCreditsMocked,
    xivaCredits,
  };
};

const createXivaErrorMessage = (subscriptionToken: string): XivaErrorMessage => ({
  method: '/push',
  params: {
    data: {
      operation: 'xivaws-error',
      message: 'error',
    },
    subscription_token: subscriptionToken,
  },
});

const createXivaDisconnectedMessage = (subscriptionToken: string): XivaDisconnectedMessage => ({
  method: '/push',
  params: {
    data: {
      event: 'disconnected',
      operation: 'disconnected',
      message: 'error',
      service: 'service',
      uid: 'uid',
    },
    subscription_token: subscriptionToken,
  },
});

const createXivaSubscribeMethodResponse = (
  subscriptionToken: string,
): XivaSubscribeMethodResponse => ({
  id: MOCKED_REQUEST_ID,
  result: {
    operation: 'attached',
    subscription_token: subscriptionToken,
  },
});

const createSimplePushWithToken = (subscriptionToken: string): XivaMessageAny =>
  ({
    method: '/push',
    params: { subscription_token: subscriptionToken },
  } as XivaMessageAny);

describe('XivaMultiplex', () => {
  describe('when websocket is not opened', () => {
    it('does not preload xiva credits', () => {
      const { multiplex, getXivaCreditsMocked } = createTestEnv();
      multiplex.subscribe().unsubscribe();

      expect(getXivaCreditsMocked).not.toBeCalled();
    });

    it('does not send message to server', () => {
      const { multiplex, sendMessageToServerMocked } = createTestEnv();
      multiplex.subscribe().unsubscribe();

      expect(sendMessageToServerMocked).not.toBeCalled();
    });

    describe('add then open', () => {
      it('tries to connect', () => {
        const { multiplex, getXivaCreditsMocked, isWebsocketOpened } = createTestEnv();

        const subscription = multiplex.subscribe();
        isWebsocketOpened.next(true);
        subscription.unsubscribe();

        expect(getXivaCreditsMocked).toBeCalled();
      });
    });
  });

  describe('when websocket is opened', () => {
    it('loads xiva credits', async () => {
      const { multiplex, getXivaCreditsMocked, isWebsocketOpened } = createTestEnv();
      isWebsocketOpened.next(true);
      const subscription = multiplex.subscribe();

      await waitFor(() => {
        expect(getXivaCreditsMocked).toBeCalled();
      });

      subscription.unsubscribe();
    });

    it('sends message to server', async () => {
      const {
        multiplex,
        sendMessageToServerMocked,
        isWebsocketOpened,
        xivaCredits,
      } = createTestEnv();
      isWebsocketOpened.next(true);
      const subscription = multiplex.subscribe();

      await waitFor(() => {
        expect(sendMessageToServerMocked).toBeCalledWith({
          method: '/subscribe',
          params: xivaCredits,
          id: '1',
        });
      });

      subscription.unsubscribe();
    });

    describe('when many observers', () => {
      it('sends message to server only once', async () => {
        const { multiplex, sendMessageToServerMocked, isWebsocketOpened } = createTestEnv();
        isWebsocketOpened.next(true);
        const subscription1 = multiplex.subscribe();
        const subscription2 = multiplex.subscribe();

        await waitFor(() => {
          expect(sendMessageToServerMocked).toBeCalledTimes(1);
        });

        subscription1.unsubscribe();
        subscription2.unsubscribe();
      });
    });

    describe('when subscription attached', () => {
      it('receives message', async () => {
        const {
          multiplex,
          sendMessageToServerMocked,
          isWebsocketOpened,
          sendMessageFromServer,
        } = createTestEnv();
        isWebsocketOpened.next(true);

        const observer = jest.fn();
        const subscription = multiplex.subscribe(observer);

        await waitFor(() => {
          expect(sendMessageToServerMocked).toBeCalled();
        });

        const subscriptionToken = 'token';
        sendMessageFromServer(createXivaSubscribeMethodResponse(subscriptionToken));
        sendMessageFromServer(createSimplePushWithToken(subscriptionToken));

        expect(observer).toBeCalledTimes(1);

        subscription.unsubscribe();
      });
    });

    describe('when it is active subscription', () => {
      [
        {
          describe: 'and xiva send xivaws-error',
          createMessageWithErrorByToken: createXivaErrorMessage,
        },
        {
          describe: 'and xiva send disconnected',
          createMessageWithErrorByToken: createXivaDisconnectedMessage,
        },
      ].forEach((config) => {
        describe(config.describe, () => {
          it('create new subscription', async () => {
            const subscriptionToken1 = 'token1';
            const subscriptionToken2 = 'token2';

            const {
              multiplex,
              sendMessageToServerMocked,
              isWebsocketOpened,
              sendMessageFromServer,
            } = createTestEnv();
            isWebsocketOpened.next(true);

            const observer = jest.fn();

            const subscription = multiplex.subscribe(observer);

            await waitFor(() => {
              expect(sendMessageToServerMocked).toBeCalled();
            });

            sendMessageFromServer(createXivaSubscribeMethodResponse(subscriptionToken1));

            sendMessageToServerMocked.mockClear();

            sendMessageFromServer(config.createMessageWithErrorByToken(subscriptionToken1));

            jest.advanceTimersByTime(XivaMultiplex.RECONNECT_DELAY_MS);
            await runPendingPromises();

            await waitFor(() => {
              expect(sendMessageToServerMocked).toBeCalled();
            });

            sendMessageFromServer(createXivaSubscribeMethodResponse(subscriptionToken2));

            sendMessageFromServer(createSimplePushWithToken(subscriptionToken1));
            sendMessageFromServer(createSimplePushWithToken(subscriptionToken2));

            expect(observer).toBeCalledTimes(1);

            subscription.unsubscribe();
          });
        });
      });
    });
  });
});
