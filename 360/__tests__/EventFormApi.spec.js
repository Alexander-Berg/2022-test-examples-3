import moment from 'moment';

import EventFormApi from '../EventFormApi';

describe('EventFormApi', () => {
  describe('getAvailabilities', () => {
    test('должен отправлять запрос на получение занятости', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.getAvailabilities({
        emails: ['test@ya.ru'],
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        exceptEventId: 100,
        instanceStartTs: Number(moment('2018-01-01T07:00:00')),
        isAllDay: true
      });

      expect(api.post).toBeCalledWith('/get-availabilities', {
        emails: ['test@ya.ru'],
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        exceptEventId: 100,
        instanceStartTs: '2018-01-01T07:00:00',
        isAllDay: true
      });
    });

    test('не должен отправлять instanceStartTs, если его не передали', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.getAvailabilities({
        emails: ['test@ya.ru'],
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        exceptEventId: 100
      });

      expect(api.post).toBeCalledWith('/get-availabilities', {
        emails: ['test@ya.ru'],
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        exceptEventId: 100
      });
    });
  });

  describe('getRepetitionDescription', () => {
    test('должен отправлять запрос на получение описания повторения', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.getRepetitionDescription({
        start: '2018-01-01T10:00:00',
        repetition: {}
      });

      expect(api.post).toBeCalledWith(
        '/get-repetition-description',
        {
          start: '2018-01-01T10:00:00',
          repetition: {}
        },
        {
          cache: true
        }
      );
    });
  });

  describe('findUsersAndResources', () => {
    test('должен отправлять запрос на поиск пользователей или переговорок', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.findUsersAndResources('test@ya.ru');

      expect(api.post).toBeCalledWith(
        '/find-users-and-resources',
        {
          loginOrEmails: 'test@ya.ru'
        },
        {
          cache: true
        }
      );
    });
  });

  describe('reserveResources', () => {
    test('должен отправлять запрос на резервирование переговорок', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.reserveResources({
        emails: ['resource@yandex-team.ru'],
        reservationId: 111,
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        exceptEventId: 100,
        instanceStartTs: Number(moment('2018-01-01T07:00:00'))
      });

      expect(api.post).toBeCalledWith('/do-reserve-resources', {
        emails: ['resource@yandex-team.ru'],
        reservationId: 111,
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        exceptEventId: 100,
        instanceStartTs: '2018-01-01T07:00:00'
      });
    });

    test('не должен отправлять instanceStartTs, если его не передали', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.reserveResources({
        emails: ['resource@yandex-team.ru'],
        reservationId: 111,
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        exceptEventId: 100
      });

      expect(api.post).toBeCalledWith('/do-reserve-resources', {
        emails: ['resource@yandex-team.ru'],
        reservationId: 111,
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        exceptEventId: 100
      });
    });
  });

  describe('cancelResourcesReservation', () => {
    test('должен отправлять запрос на отмену резервирования переговорок', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.cancelResourcesReservation(111);

      expect(api.post).toBeCalledWith('/do-cancel-resources-reservation', {reservationId: 111});
    });
  });

  describe('createConferenceCall', () => {
    test('должен отправлять запрос на создание конференц-звонка', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.createConferenceCall({
        duration: 10,
        phones: '111,222'
      });

      expect(api.post).toBeCalledWith('/do-create-conference-call', {
        duration: 10,
        phones: '111,222'
      });
    });
  });

  describe('getAttendees', () => {
    test('должен отправлять запрос получение полного списка участников', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.getAttendees(1);

      expect(api.post).toBeCalledWith('/get-attendees', {eventId: 1});
    });
  });

  describe('getTelemostConferenceLink', () => {
    test('должен запрашивать ссылку на конференцию с телемоста', () => {
      const api = {
        post: jest.fn()
      };
      const eventFormApi = new EventFormApi(api);

      eventFormApi.getTelemostConferenceLink();

      expect(api.post).toBeCalledWith('/get-conference-link');
    });
  });
});
