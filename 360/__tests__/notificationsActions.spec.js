import Notifications from 'react-notification-system-redux';

import {
  notifyFailure,
  notifySuccess,
  notificationHelpers,
  getDefaultParams
} from '../notificationsActions';

jest.mock('utils/i18n');

describe('notificationsActions', () => {
  describe('getDefaultParams', () => {
    test('Дефолтные параметры нотификаций должны предполагать кнопку закрытия', () => {
      expect(getDefaultParams()).toEqual({
        dismissible: 'button',
        position: 'tr'
      });
    });
  });
  describe('notifyFailure', () => {
    beforeEach(() => {
      jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
      jest.spyOn(Notifications, 'error').mockReturnValue();
    });

    test('должен быть вызван с дефолтными параметрами, если ничего не передали', () => {
      notifyFailure();
      expect(Notifications.error).toBeCalledWith({
        dismissible: 'button',
        position: 'tr',
        message: 'errors.defaultError',
        autoDismiss: 5,
        uid: 'id'
      });
    });

    test('должен быть вызван с кастомными параметрам, если их передали', () => {
      notifyFailure({
        position: 'bl',
        autoDismiss: 10
      });

      expect(Notifications.error).toBeCalledWith({
        dismissible: 'button',
        position: 'bl',
        autoDismiss: 10,
        message: 'errors.defaultError',
        uid: 'id'
      });
    });

    test('должен быть вызван с параметрами для event-starts-after-due', () => {
      notifyFailure({error: {name: 'event-starts-after-due'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.eventStartsAfterDue');
    });

    test('должен быть вызван с параметрами для event-not-found', () => {
      notifyFailure({error: {name: 'event-not-found'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.eventNotFound');
    });

    test('должен быть вызван с параметрами для event-longer-than-rep', () => {
      notifyFailure({error: {name: 'event-longer-than-rep'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.eventLongerThanRep');
    });

    test('должен быть вызван с параметрами для event-modified', () => {
      notifyFailure({error: {name: 'event-modified'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.eventModified');
    });

    test('должен быть вызван с параметрами для ics-feed-invalid-url', () => {
      notifyFailure({error: {name: 'ics-feed-invalid-url'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.icsFeedInvalidUrl');
    });

    test('должен быть вызван с параметрами для ics-parsing-error', () => {
      notifyFailure({error: {name: 'ics-parsing-error'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.icsParsingError');
    });

    test('должен быть вызван с параметрами для ics-feed-already-subscribed-url', () => {
      notifyFailure({error: {name: 'ics-feed-already-subscribed-url'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe(
        'errors.icsFeedAlreadySubscribedUrl'
      );
    });

    test('должен быть вызван с параметрами для inv-is-missing', () => {
      notifyFailure({error: {name: 'inv-is-missing'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.invIsMissing');
    });

    test('должен быть вызван с параметрами для bad-karma', () => {
      notifyFailure({error: {name: 'bad-karma'}});

      expect(Notifications.error.mock.calls[0][0].message).toBeFalsy();
      expect(Notifications.error.mock.calls[0][0].children).toMatchSnapshot();
    });

    test('должен быть вызван с параметрами для spam-detected', () => {
      notifyFailure({error: {name: 'spam-detected'}});

      expect(Notifications.error.mock.calls[0][0].message).toBeFalsy();
      expect(Notifications.error.mock.calls[0][0].children).toMatchSnapshot();
    });

    test('должен быть вызван с параметрами для busy-overlap', () => {
      notifyFailure({error: {name: 'busy-overlap'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.busyOverlap');
    });

    test('должен быть вызван с параметрами для too-long-event', () => {
      notifyFailure({error: {name: 'too-long-event'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.tooLongEvent');
    });

    test('должен быть вызван с параметрами для too-short-event', () => {
      notifyFailure({error: {name: 'too-short-event'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.tooShortEvent');
    });

    test('должен быть вызван с параметрами для ews-occurrences-overlap', () => {
      notifyFailure({error: {name: 'ews-occurrences-overlap'}});

      expect(Notifications.error.mock.calls[0][0].message).toBe('errors.ewsOccurrencesOverlap');
    });

    test('должен показывать присланный текст ошибки, если он есть', () => {
      notifyFailure({
        error: {
          readable: {
            ru: 'ошибка'
          }
        }
      });

      expect(Notifications.error.mock.calls[0][0].message).toBe('ошибка');
    });

    describe('Код ошибки', () => {
      test('должен добавлять reqid в сообщение, если он есть', () => {
        notifyFailure({
          error: {
            name: 'too-short-event',
            reqid: 'sa7d8as7dasd8as8d8asd9asd5652a1c'
          }
        });

        expect(Notifications.error.mock.calls[0][0].message).toBe(
          'errors.tooShortEvent\nerrors.code'
        );
      });

      test('должен добавлять reqid в дефолтное сообщение', () => {
        notifyFailure({
          error: {
            name: 'unknown',
            reqid: 'sa7d8as7dasd8as8d8asd9asd5652a1c'
          }
        });

        expect(Notifications.error.mock.calls[0][0].message).toBe(
          'errors.defaultError\nerrors.code'
        );
      });

      test('не должен добавлять reqid, если нет сообщения', () => {
        notifyFailure({
          error: {
            name: 'bad-karma',
            reqid: 'sa7d8as7dasd8as8d8asd9asd5652a1c'
          }
        });

        expect(Notifications.error.mock.calls[0][0].message).toBe(null);
      });

      test('не должен добавлять reqid в сообщение, если его нет', () => {
        notifyFailure({error: {name: 'too-short-event'}});

        expect(Notifications.error.mock.calls[0][0].message).toBe('errors.tooShortEvent');
      });
    });
  });

  describe('notifySuccess', () => {
    beforeEach(() => {
      jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
      jest.spyOn(Notifications, 'success').mockReturnValue();
    });

    test('должен вызываться с дефолтными параметрами, если ничего не передали', () => {
      notifySuccess();

      expect(Notifications.success).toBeCalledWith({
        dismissible: 'button',
        position: 'tr',
        autoDismiss: 2,
        uid: 'id'
      });
    });

    test('должен вызываться с кастомными параметрами, если их передали', () => {
      const params = {
        dismissible: true,
        position: 'bl',
        autoDismiss: 10
      };

      notifySuccess(params);

      expect(Notifications.success).toBeCalledWith({
        dismissible: true,
        position: 'bl',
        autoDismiss: 10,
        uid: 'id'
      });
    });
  });
});
