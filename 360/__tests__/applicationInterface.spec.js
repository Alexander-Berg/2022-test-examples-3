import * as environment from 'configs/environment';

import mobileAppManager, {
  MobileAppManager,
  applicationInterface,
  maybeSetupWebView
} from '../applicationInterface';

const applicationInterfaceSetup = require('../../../shared/index/helpers/application-interface');

describe('applicationInterface', () => {
  beforeEach(() => {
    sinon.stub(environment, 'isIosApp').value(false);
    sinon.stub(environment, 'isAndroidApp').value(false);
    sinon.stub(environment, 'isMobileApp').value(false);
  });

  describe('applicationInterface', () => {
    test('должен отправлять события на ios', () => {
      applicationInterfaceSetup(window, {appType: 'ios'});

      const messageSender = jest.fn();
      const signalName = 'ololo';
      const value = 'test link';
      sinon.stub(window.webkit.messageHandlers.ololo, 'postMessage').value(messageSender);

      applicationInterface(signalName, value);

      expect(window.webkit.messageHandlers.ololo.postMessage).toHaveBeenCalledTimes(1);
      expect(window.webkit.messageHandlers.ololo.postMessage).toHaveBeenCalledWith(value);
    });

    test('должен отправлять события на android', () => {
      applicationInterfaceSetup(window, {appType: 'android'});

      const messageSender = jest.fn();
      const message = 'ololo';
      sinon.stub(window.mail, 'onEvent').value(messageSender);

      applicationInterface(message);

      expect(window.mail.onEvent).toBeCalledWith(message, undefined);
    });

    test('не должен ломаться, если нет нужных методов в window', () => {
      applicationInterfaceSetup(window, {appType: 'ios'});

      const message = 'ololo';
      sinon.stub(window, 'webkit').value(undefined);
      sinon.stub(window, 'mail').value(undefined);

      applicationInterface(message);

      sinon.stub(environment, 'isIosApp').value(true);
      applicationInterface(message);

      // просто проверяем, что не было exception
      expect(1).toEqual(1);
    });
  });

  describe('mobileAppManager', () => {
    describe('goBack', () => {
      test('должен вызывать history.back, если нет обработчиков в очереди', () => {
        const mobileAppManager = new MobileAppManager();
        const historyBack = jest.fn();
        sinon.stub(window.history, 'back').value(historyBack);

        mobileAppManager.goBack();

        expect(window.history.back).toHaveBeenCalledTimes(1);
      });
      test('не должен вызывать history.back, если есть обработчики в очереди', () => {
        const mobileAppManager = new MobileAppManager();
        mobileAppManager.registerBackButtonHandler(() => {});
        const historyBack = jest.fn();
        sinon.stub(window.history, 'back').value(historyBack);

        mobileAppManager.goBack();

        expect(window.history.back).toHaveBeenCalledTimes(0);
      });
      test('должен вызывать последний обработчик в очереди', () => {
        const mobileAppManager = new MobileAppManager();
        const handler1 = jest.fn();
        const handler2 = jest.fn();
        const handler3 = jest.fn();

        mobileAppManager.registerBackButtonHandler(handler1);
        mobileAppManager.registerBackButtonHandler(handler2);
        mobileAppManager.registerBackButtonHandler(handler3);

        mobileAppManager.goBack();

        expect(handler3).toHaveBeenCalledTimes(1);
      });
    });

    describe('registerBackButtonHandler', () => {
      test('должен добавлять обработчики в очередь', () => {
        const mobileAppManager = new MobileAppManager();

        const handler1 = () => {};
        const handler2 = () => {};

        mobileAppManager.registerBackButtonHandler(handler1);
        mobileAppManager.registerBackButtonHandler(handler2);

        expect(mobileAppManager._backButtonHandlers).toEqual([handler1, handler2]);
      });
    });

    describe('unregisterBackButtonHandler', () => {
      test('должен удалять последний обработчик из очереди, если ничего не передали', () => {
        const mobileAppManager = new MobileAppManager();

        const handler1 = () => {};
        const handler2 = () => {};

        mobileAppManager.registerBackButtonHandler(handler1);
        mobileAppManager.registerBackButtonHandler(handler2);

        mobileAppManager.unregisterBackButtonHandler();

        expect(mobileAppManager._backButtonHandlers).toEqual([handler1]);
      });
      test('должен удалять переданный обработчик из очереди', () => {
        const mobileAppManager = new MobileAppManager();

        const handler1 = () => {};
        const handler2 = () => {};
        const handler3 = () => {};

        mobileAppManager.registerBackButtonHandler(handler1);
        mobileAppManager.registerBackButtonHandler(handler2);
        mobileAppManager.registerBackButtonHandler(handler3);

        mobileAppManager.unregisterBackButtonHandler(handler2);

        expect(mobileAppManager._backButtonHandlers).toEqual([handler1, handler3]);
      });
    });
  });
  describe('maybeSetupWebView', () => {
    afterEach(() => {
      delete window.calendar;
    });

    test('должен записывать в window.calendar mobileAppManager', async () => {
      sinon.stub(environment, 'isMobileApp').value(true);

      maybeSetupWebView();

      return expect(window.calendar).toEqual(mobileAppManager);
    });

    test('не должен записывать в window.calendar mobileAppManager, если не mobileApp', async () => {
      sinon.stub(environment, 'isMobileApp').value(false);
      const manager = {manager: true};
      maybeSetupWebView(manager);

      return expect(window.calendar).toBe(undefined);
    });
  });
});
