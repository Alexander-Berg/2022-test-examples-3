import moment from 'moment';

import EventsApi from '../EventsApi';

describe('EventsApi', () => {
  describe('create', () => {
    test('должен отправлять запрос на создание события', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.create({
        name: 'event',
        start: Number(moment('2018-01-01T10:00')),
        end: Number(moment('2018-01-01T10:30'))
      });

      expect(api.post).toBeCalledWith('/create-event', {
        name: 'event',
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T10:30:00'
      });
    });
  });

  describe('update', () => {
    test('должен отправлять запрос на обновление события', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.update({
        newEvent: {
          id: 1,
          start: Number(moment('2018-01-01T11:00')),
          end: Number(moment('2018-01-01T12:00')),
          instanceStartTs: null,
          layerId: 2,
          repetition: {weekly: true}
        },
        oldEvent: {
          id: 1,
          start: Number(moment('2018-01-01T10:00')),
          end: Number(moment('2018-01-01T10:30')),
          instanceStartTs: Number(moment('2018-01-01T07:00')),
          layerId: 1,
          repetition: {weekly: true}
        },
        mailToAll: false,
        applyToFuture: true
      });

      expect(api.post).toBeCalledWith('/update-event', {
        id: 1,
        start: '2018-01-01T11:00:00',
        end: '2018-01-01T12:00:00',
        instanceStartTs: '2018-01-01T07:00:00',
        layerId: 2,
        repetition: {weekly: true},
        extraQuery: {
          layerId: 1
        },
        mailToAll: false,
        applyToFuture: true
      });
    });

    test('не должен отправлять instanceStartTs и applyToFuture, если отредактировали событие без повторения', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.update({
        newEvent: {
          id: 1,
          start: Number(moment('2018-01-01T11:00')),
          end: Number(moment('2018-01-01T12:00')),
          instanceStartTs: Number(moment('2018-01-01T07:00')),
          layerId: 1,
          repetition: {weekly: true}
        },
        oldEvent: {
          id: 1,
          start: Number(moment('2018-01-01T10:00')),
          end: Number(moment('2018-01-01T10:30')),
          instanceStartTs: Number(moment('2018-01-01T07:00')),
          layerId: 1
        },
        mailToAll: false,
        applyToFuture: true
      });

      expect(api.post).toBeCalledWith('/update-event', {
        id: 1,
        start: '2018-01-01T11:00:00',
        end: '2018-01-01T12:00:00',
        layerId: 1,
        repetition: {weekly: true},
        extraQuery: {
          layerId: 1
        },
        mailToAll: false
      });
    });
  });

  describe('updateDecision', () => {
    test('должен отправлять запрос на обновление решения', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.updateDecision({
        event: {
          id: 1,
          instanceStartTs: Number(moment('2018-01-01T07:00'))
        },
        decision: 'no',
        reason: 'because',
        applyToAll: false
      });

      expect(api.post).toBeCalledWith('/handle-reply', {
        eventId: 1,
        instanceStartTs: '2018-01-01T07:00:00',
        decision: 'no',
        reason: 'because',
        applyToAll: false
      });
    });

    test('не должен отправлять instanceStartTs, если это обновление решения для всей серии', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.updateDecision({
        event: {
          id: 1,
          instanceStartTs: Number(moment('2018-01-01T07:00'))
        },
        decision: 'no',
        reason: 'because',
        applyToAll: true
      });

      expect(api.post).toBeCalledWith('/handle-reply', {
        eventId: 1,
        decision: 'no',
        reason: 'because',
        applyToAll: true
      });
    });
  });

  describe('delete', () => {
    test('должен отправлять запрос на удаление', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.delete({
        event: {
          id: 1,
          instanceStartTs: Number(moment('2018-01-01T07:00')),
          sequence: 2
        },
        applyToFuture: false
      });

      expect(api.post).toBeCalledWith('/delete-event', {
        id: 1,
        instanceStartTs: '2018-01-01T07:00:00',
        sequence: 2,
        applyToFuture: false
      });
    });

    test('не должен отправлять instaceStartTs, если это удаление всей серии', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.delete({
        event: {
          id: 1,
          instanceStartTs: Number(moment('2018-01-01T07:00')),
          sequence: 2
        },
        applyToFuture: true
      });

      expect(api.post).toBeCalledWith('/delete-event', {
        id: 1,
        sequence: 2,
        applyToFuture: true
      });
    });
  });

  describe('detach', () => {
    test('должен отправлять запрос на удаление чужого события', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.detach({
        id: 1,
        layerId: 2
      });

      expect(api.post).toBeCalledWith('/do-detach-event', {
        id: 1,
        layerId: 2
      });
    });
  });

  describe('confirmRepetition', () => {
    test('должен отправлять запрос на подтверждение повторяющегося события', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.confirmRepetition({id: 1});

      expect(api.post).toBeCalledWith('/do-confirm-event-repetition', {id: 1});
    });
  });

  describe('getEvents', () => {
    test('должен отправлять запрос на получение событий', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.getEvents({
        from: Number(moment('2018-01-01')),
        to: Number(moment('2018-01-07')),
        layerId: [1, 2],
        externalId: ['externalId']
      });

      expect(api.post).toBeCalledWith('/get-events', {
        from: '2018-01-01',
        to: '2018-01-07',
        layerId: [1, 2],
        limitAttendees: true,
        externalId: ['externalId']
      });
    });
  });

  describe('getModifiedEvents', () => {
    test('должен отправлять запрос на получение измененных событий', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.getModifiedEvents({
        from: Number(moment('2018-01-01')),
        to: Number(moment('2018-01-07')),
        since: Number(moment('2018-01-02T10:00'))
      });

      expect(api.post).toBeCalledWith('/get-modified-events', {
        from: '2018-01-01',
        to: '2018-01-07',
        since: Number(moment('2018-01-02T10:00'))
      });
    });
  });

  describe('getEvent', () => {
    test('должен отправлять запрос на получение события', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.getEvent({
        eventId: 1,
        layerId: 2,
        instanceStartTs: '2018-01-02T10:00:00',
        recurrenceAsOccurrence: true
      });

      expect(api.post).toBeCalledWith('/get-event', {
        eventId: 1,
        layerId: 2,
        instanceStartTs: '2018-01-02T10:00:00',
        recurrenceAsOccurrence: true
      });
    });
  });

  describe('getEventsByLogin', () => {
    test('должен отправить запрос на получения события по логину', () => {
      const api = {
        post: jest.fn()
      };
      const eventsApi = new EventsApi(api);

      eventsApi.getEventsByLogin({
        from: Number(moment('2018-01-01')),
        to: Number(moment('2018-01-07')),
        layerId: [1, 2],
        externalId: ['externalId'],
        login: 'login',
        opaqueOnly: true
      });

      expect(api.post).toBeCalledWith('/get-events-by-login', {
        from: '2018-01-01',
        to: '2018-01-07',
        layerId: [1, 2],
        limitAttendees: true,
        externalId: ['externalId'],
        login: 'login',
        opaqueOnly: true
      });
    });
  });
});
