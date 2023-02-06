import queryString from 'query-string';
import {Server} from 'mock-socket';

import XivaApi from '../XivaApi';

describe('XivaApi', () => {
  const baseUri = 'wss://localhost:8080';
  const options = {
    user: 'user_id',
    session: 'session_1',
    service: 'calendar',
    client: 'maya',
    fetch_history: 'user_id:calendar:0:5'
  };
  const url = `${baseUri}/v2/subscribe/websocket?${queryString.stringify(options)}`;

  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllTimers();
  });

  describe('subscribe', () => {
    test('должен открывать соединение и обрабатывать полученные сообщения', () => {
      const mockServer = new Server(url);
      mockServer.on('connection', () => {
        mockServer.send(JSON.stringify({operation: 'test'}));
      });

      const xivaApi = new XivaApi(baseUri);
      const onMessage = jest.fn();
      xivaApi.subscribe({...options, onMessage});

      jest.advanceTimersByTime(100);

      mockServer.stop();

      expect(onMessage).toHaveBeenCalledTimes(1);
      expect(onMessage).toHaveBeenCalledWith({operation: 'test'});
    });

    test('должен открывать только одно соединение', () => {
      const mockServer = new Server(url);
      const onConnection = jest.fn();
      mockServer.on('connection', onConnection);

      const xivaApi = new XivaApi(baseUri);
      xivaApi.subscribe({...options, onMessage: jest.fn()});
      xivaApi.subscribe({...options, onMessage: jest.fn()});

      jest.advanceTimersByTime(100);

      mockServer.stop();

      expect(onConnection).toHaveBeenCalledTimes(1);
    });

    describe('reconnect', () => {
      test('должен делать заданное количество перезапросов при потере соединения', () => {
        const mockServer = new Server(url);
        mockServer.on('connection', () => {
          mockServer.close();
        });

        const xivaApi = new XivaApi(baseUri, {
          reconnectionAttempts: 10
        });
        xivaApi.subscribe({...options, onMessage: jest.fn()});

        jest.spyOn(xivaApi, 'subscribe');
        jest.runAllTimers();

        mockServer.stop();

        expect(xivaApi.subscribe).toHaveBeenCalledTimes(10);
      });

      test('должен запрашивать пропущенные сообщения при потере соединения', () => {
        const mockServer = new Server(url);
        mockServer.on('connection', () => {
          mockServer.close();
        });

        const xivaApi = new XivaApi(baseUri, {
          reconnectionAttempts: 1
        });
        xivaApi._positions.calendar = 4000;
        xivaApi.subscribe({...options, onMessage: jest.fn()});

        jest.spyOn(xivaApi, 'subscribe');
        jest.runAllTimers();

        mockServer.stop();

        expect(xivaApi.subscribe).toHaveBeenCalledWith(
          expect.objectContaining({
            fetch_history: 'user_id:calendar:4000:5'
          })
        );
      });
    });

    describe('ping', () => {
      test('должен обрабатывать ping', () => {
        const mockServer = new Server(url);
        const onClose = jest.fn();
        mockServer.on('connection', () => {
          mockServer.send(
            JSON.stringify({
              operation: 'ping',
              'server-interval-sec': 1
            })
          );
        });
        mockServer.on('close', onClose);

        const xivaApi = new XivaApi(baseUri);
        xivaApi.subscribe({...options, onMessage: jest.fn()});

        jest.spyOn(xivaApi, 'subscribe');
        jest.advanceTimersByTime(4000);

        mockServer.stop();

        expect(onClose).toHaveBeenCalledTimes(1);
        expect(xivaApi.subscribe).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe('unsubscribe', () => {
    test('должен закрывать соединение', () => {
      const mockServer = new Server(url);
      const onClose = jest.fn();
      mockServer.on('connection', jest.fn());
      mockServer.on('close', onClose);

      const xivaApi = new XivaApi(baseUri);
      xivaApi.subscribe({...options, onMessage: jest.fn()});

      jest.advanceTimersByTime(100);

      xivaApi.unsubscribe();

      mockServer.stop();

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    test('не должен закрывать соединение, если его нет', () => {
      const mockServer = new Server(url);
      const onClose = jest.fn();
      mockServer.on('connection', jest.fn());
      mockServer.on('close', onClose);

      const xivaApi = new XivaApi(baseUri);

      xivaApi.unsubscribe();

      mockServer.stop();

      expect(onClose).toHaveBeenCalledTimes(0);
    });
  });
});
