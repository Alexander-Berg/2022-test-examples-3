import { Subject, Subscription } from 'rxjs';
import { logger } from 'services/Logger';
import { mocked } from 'ts-jest/utils';
import { webSocket, WebSocketSubject, WebSocketSubjectConfig } from 'rxjs/webSocket';
import { XivaMessageAny } from 'types/xiva/XivaWebsocketJSONApi';
import { XivaClient } from './XivaClient';
import { XivaAliveCheck } from './XivaAliveCheck';

jest.mock('services/Logger');
jest.mock('rxjs/webSocket');
jest.mock('./XivaAliveCheck');

jest.useFakeTimers('modern');

const loggerMocked = mocked(logger);
const webSocketMocked = mocked(webSocket);
const XivaAliveCheckMocked = mocked(XivaAliveCheck);

let fromServer: Subject<string>;
let toServer: Subject<string>;

let config: WebSocketSubjectConfig<string> = { url: '' };

webSocketMocked.mockImplementation((_config: WebSocketSubjectConfig<string>) => {
  config = _config;

  if (config.openObserver) {
    config.openObserver.next(new Event('open'));
  }

  fromServer = new Subject<string>();
  toServer = new Subject<string>();

  const next = toServer.next.bind(toServer);

  const proxy = new Proxy(fromServer, {
    get(target, prop, receiver) {
      if (prop === 'next') {
        return next;
      }
      return Reflect.get(target, prop, receiver);
    },
  });

  return proxy as WebSocketSubject<string>;
});

let xivaAliveCheckMocked = new Subject<never>();
XivaAliveCheckMocked.create.mockImplementation(() => {
  xivaAliveCheckMocked = new Subject<never>();
  return xivaAliveCheckMocked.asObservable();
});

let subscription = new Subscription();

beforeEach(() => {
  subscription.unsubscribe();
  subscription = new Subscription();

  webSocketMocked.mockClear();

  loggerMocked.reportInfo.mockClear();
  loggerMocked.reportError.mockClear();
});

describe('XivaClient', () => {
  it('receives message from server', () => {
    const xivaClient = new XivaClient();
    const next = jest.fn();

    subscription.add(xivaClient.messageFromServer.subscribe(next));
    fromServer.next('message');

    expect(next).toBeCalledWith('message');
  });

  it('sends message to server', () => {
    const xivaClient = new XivaClient();
    const next = jest.fn();

    subscription.add(xivaClient.messageFromServer.subscribe(next));
    subscription.add(toServer.subscribe(next));

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    xivaClient.next('message' as any);

    expect(next).toBeCalledWith('message');
  });

  describe('when websocket throws error', () => {
    it('creates new connection', () => {
      const xivaClient = new XivaClient();
      const next = jest.fn();
      subscription.add(xivaClient.messageFromServer.subscribe(next));

      fromServer.error(new Error());

      jest.advanceTimersByTime(1000);
      fromServer.next('message');

      expect(webSocketMocked).toBeCalledTimes(2);
      expect(next).toBeCalledWith('message');
    });
  });

  describe('when keepAlive throws error', () => {
    it('creates new connection', () => {
      const xivaClient = new XivaClient();
      const next = jest.fn();
      subscription.add(xivaClient.messageFromServer.subscribe(next));

      xivaAliveCheckMocked.error(new Error());

      jest.advanceTimersByTime(1000);
      fromServer.next('message');

      subscription.unsubscribe();

      expect(webSocketMocked).toBeCalledTimes(2);
      expect(next).toBeCalledWith('message');
    });
  });

  describe('when reconnecting', () => {
    it('updates isWebsocketOpened', () => {
      const xivaClient = new XivaClient();
      subscription.add(xivaClient.messageFromServer.subscribe(() => {}));

      expect(xivaClient.isWebsocketOpened.value).toBe(true);
      fromServer.error(new Error());
      expect(xivaClient.isWebsocketOpened.value).toBe(false);

      jest.advanceTimersByTime(1000);
      expect(xivaClient.isWebsocketOpened.value).toBe(true);
    });
  });

  describe('when it work normal', () => {
    let xivaClient: XivaClient<XivaMessageAny>;
    beforeEach(() => {
      xivaClient = new XivaClient();
      subscription.add(xivaClient.messageFromServer.subscribe(() => {}));
    });

    it('logs close websocket event', () => {
      config.closingObserver?.next();
      expect(loggerMocked.reportInfo).toBeCalled();
    });

    it('logs open websocket event', () => {
      config.openObserver?.next(new Event('open'));
      expect(loggerMocked.reportInfo).toBeCalled();
    });

    it('logs server message from', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      xivaClient.next('message' as any);
      expect(loggerMocked.reportInfo).toBeCalled();
    });

    it('logs server message to', () => {
      fromServer.next('message');
      expect(loggerMocked.reportInfo).toBeCalled();
    });
  });
});
