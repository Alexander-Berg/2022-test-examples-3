import config from 'configs/config';
import * as session from 'configs/session';
import * as environment from 'configs/environment';

import {LoggingApi, LoggingModelsApi} from '../LoggingApi';
import {ActionNames} from '../loggingConstants';

const noop = () => {};

describe('LoggingApi', () => {
  describe('LoggingModelsApi', () => {
    describe('aceventuraSuggestReport', () => {
      test('должен отправлять данные о выборе из саджеста', () => {
        const api = {
          post: jest.fn()
        };
        const loggingModelsApi = new LoggingModelsApi(api);
        const text = '123123';
        const contact = {
          contact_id: 777,
          email: 'pew@ya.ru'
        };
        const user_type = 'common';

        sinon.stub(config.user, 'type').value(user_type);

        loggingModelsApi.aceventuraSuggestReport({
          inputValue: text,
          contact
        });

        expect(api.post).toBeCalledTimes(1);
        expect(api.post).toBeCalledWith('/aceventura-suggest-report', {
          q: text,
          email: contact.email,
          contact_id: contact.contact_id,
          user_type
        });
      });
    });
  });

  describe('LoggingApi', () => {
    describe('_log', () => {
      test('должен вызывать api.post с общими параметрами', () => {
        const api = new LoggingApi();
        const actionName = 'action_name';
        const eventId = Symbol();
        const instanceStartTs = Date.now();
        const clientVer = 'version';
        const eexp = Symbol();
        const platform = Symbol();
        const isTouch = Symbol();

        sinon.stub(session, 'version').value(clientVer);
        sinon.stub(environment, 'appType').value(platform);
        sinon.stub(environment, 'isMobileApp').value(true);
        sinon.stub(environment, 'isTouch').value(isTouch);
        sinon.stub(config.experiments, 'eexp').value(eexp);

        jest.spyOn(api.api, 'post').mockImplementation(noop);
        api._log(actionName, {eventId, instanceStartTs});

        expect(api.api.post).toHaveBeenCalledTimes(1);
        expect(api.api.post).toHaveBeenCalledWith(
          '/journal',
          {
            params: {},
            eventId,
            instanceStartTs,
            clientVer,
            actionName,
            platform,
            isTouch,
            eexp
          },
          {}
        );
      });

      test('должен возвращать результат api.post', () => {
        const api = new LoggingApi();
        const actionName = 'action_name';
        const expectedReturnValue = Symbol();

        jest.spyOn(api.api, 'post').mockReturnValue(expectedReturnValue);

        expect(api._log(actionName, {})).toBe(expectedReturnValue);
      });
    });

    describe('logCreateEvent', () => {
      test('должен вызывать _log с нужными параметрами', () => {
        const api = new LoggingApi();
        const eventId = 12345;

        jest.spyOn(api, '_log').mockImplementation(noop);
        api.logCreateEvent({eventId});

        expect(api._log).toHaveBeenCalledTimes(1);
        expect(api._log).toHaveBeenCalledWith(ActionNames.CREATE_EVENT, {eventId});
      });

      test('должен возвращать результат _log', () => {
        const api = new LoggingApi();
        const eventId = 12345;
        const logReturnValue = Symbol();

        jest.spyOn(api, '_log').mockReturnValue(logReturnValue);

        expect(api.logCreateEvent({eventId})).toBe(logReturnValue);
      });
    });

    describe('logDeleteEvent', () => {
      test('должен вызывать _log с нужными параметрами', () => {
        const api = new LoggingApi();
        const eventId = 12345;
        const instanceStartTs = Date.now();

        jest.spyOn(api, '_log').mockImplementation(noop);
        api.logDeleteEvent({eventId, instanceStartTs});

        expect(api._log).toHaveBeenCalledTimes(1);
        expect(api._log).toHaveBeenCalledWith(ActionNames.DELETE_EVENT, {eventId, instanceStartTs});
      });

      test('должен возвращать результат _log', () => {
        const api = new LoggingApi();
        const eventId = 12345;
        const instanceStartTs = Date.now();
        const logReturnValue = Symbol();

        jest.spyOn(api, '_log').mockReturnValue(logReturnValue);

        expect(api.logCreateEvent({eventId, instanceStartTs})).toBe(logReturnValue);
      });
    });

    describe('logUpdateEvent', () => {
      test('должен вызывать _log с нужными параметрами', () => {
        const api = new LoggingApi();
        const eventId = 12345;
        const instanceStartTs = Date.now();

        jest.spyOn(api, '_log').mockImplementation(noop);
        api.logUpdateEvent({eventId, instanceStartTs});

        expect(api._log).toHaveBeenCalledTimes(1);
        expect(api._log).toHaveBeenCalledWith(ActionNames.UPDATE_EVENT, {eventId, instanceStartTs});
      });

      test('должен возвращать результат _log', () => {
        const api = new LoggingApi();
        const eventId = 12345;
        const instanceStartTs = Date.now();
        const logReturnValue = Symbol();

        jest.spyOn(api, '_log').mockReturnValue(logReturnValue);

        expect(api.logUpdateEvent({eventId, instanceStartTs})).toBe(logReturnValue);
      });
    });
  });
});
