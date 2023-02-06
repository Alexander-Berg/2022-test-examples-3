import {pick} from 'lodash';

import processOutputRepetition from 'features/eventForm/utils/processOutputRepetition';

import YupiApi from '../YupiApi';

jest.mock('features/eventForm/utils/processOutputRepetition');

describe('YupiApi', () => {
  describe('getAvailabilityIntervals', () => {
    test('должен отправлять запрос за моделью', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1);

      yupiApi.getAvailabilityIntervals({
        emails: ['x@y.z'],
        date,
        exceptEventId: 1
      });

      expect(api.post).toHaveBeenCalledTimes(1);
      expect(api.post).toBeCalledWith('/get-availability-intervals', {
        emails: ['x@y.z'],
        date: '2020-01-01',
        shape: 'ids-only',
        exceptEventId: 1
      });
    });
  });
  describe('getResourcesSchedule', () => {
    test('должен отправлять запрос за моделью', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1);

      yupiApi.getResourcesSchedule({
        email: 'x@y.z',
        date,
        exceptEventId: 1,
        filter: '123',
        officeId: 1
      });

      expect(api.post).toHaveBeenCalledTimes(1);
      expect(api.post).toBeCalledWith('/get-resources-schedule', {
        email: 'x@y.z',
        date: '2020-01-01',
        exceptEventId: 1,
        filter: '123',
        officeId: 1
      });
    });
  });
  describe('suggestMeetingResources', () => {
    test('должен отправлять запрос за моделью', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1, 10, 15);
      const event = {
        resources: []
      };

      yupiApi.suggestMeetingResources(event, date, false);

      expect(api.post).toHaveBeenCalledTimes(1);
      expect(api.post.mock.calls[0][0]).toEqual('/suggest-meeting-resources');
    });
    test('должен отправлять общие параметры', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1);
      const event = {
        resources: [],
        id: 10,
        ignoreUsersEvents: true
      };
      const expectedParams = {
        numberOfOptions: 5,
        exceptEventId: 10,
        ignoreUsersEvents: true
      };

      yupiApi.suggestMeetingResources(event, date, false);

      expect(
        pick(api.post.mock.calls[0][1], ['numberOfOptions', 'ignoreUsersEvents', 'exceptEventId'])
      ).toEqual(expectedParams);
    });
    test('должен отправлять repetition при переданном shouldUseRepetition', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const repetition = 'REPETITION';
      const date = new Date(2020, 0, 1, 10, 15);
      const event = {
        resources: []
      };

      processOutputRepetition.mockImplementation(() => repetition);
      yupiApi.suggestMeetingResources(event, date, true);

      expect(api.post.mock.calls[0][1].repetition).toEqual(repetition);
    });
    test('должен приводить даты к нужному формату', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1);
      const start = new Date(2020, 0, 1, 10, 15);
      const end = new Date(2020, 0, 1, 10, 45);
      const event = {
        resources: [],
        start,
        end,
        instanceStartTs: start
      };
      const expectedParams = {
        date: '2020-01-01',
        selectedStart: '2020-01-01T10:15',
        instanceStartTs: '2020-01-01T10:15:00',
        eventStart: '2020-01-01T10:15',
        eventEnd: '2020-01-01T10:45'
      };

      yupiApi.suggestMeetingResources(event, date, false);

      expect(
        pick(api.post.mock.calls[0][1], [
          'date',
          'selectedStart',
          'eventStart',
          'eventEnd',
          'instanceStartTs'
        ])
      ).toEqual(expectedParams);
    });
    test('должен отправлять имейлы участников встречи', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1);
      const event = {
        resources: [],
        attendees: [{email: '1@ya.ru'}, {email: '2@ya.ru'}],
        organizer: {email: '0@ya.ru'}
      };

      yupiApi.suggestMeetingResources(event, date, false);

      expect(api.post.mock.calls[0][1].users).toEqual(['0@ya.ru', '1@ya.ru', '2@ya.ru']);
    });
    test('не должен отправлять instanceStartTs, если его нет в событии', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1);
      const event = {resources: []};

      yupiApi.suggestMeetingResources(event, date, false);

      expect(api.post.mock.calls[0][1].instanceStartTs).toEqual(undefined);
    });
    test('должен корректно отрабатывать при отсутсвиии resourcesFilter у события', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1);
      const resourceEmail = 'resource@y-t.ru';
      const event = {
        resources: [
          {
            officeId: 5,
            resource: {
              officeId: 5,
              email: resourceEmail
            }
          }
        ]
      };

      yupiApi.suggestMeetingResources(event, date, false);

      expect(api.post.mock.calls[0][1].offices).toEqual([
        {
          id: 5,
          filter: '',
          selectedResourceEmails: [resourceEmail]
        }
      ]);
    });
    test('должен отправлять информацию об офисах', () => {
      const api = {
        post: jest.fn()
      };
      const yupiApi = new YupiApi(api);
      const date = new Date(2020, 0, 1);
      const resourcesFilter = 'resourcesFilter';
      const resourceEmail = 'resource@y-t.ru';
      const event = {
        resourcesFilter: {
          5: resourcesFilter
        },
        resources: [
          {
            officeId: 5,
            resource: {
              officeId: 5,
              email: resourceEmail
            }
          }
        ]
      };

      yupiApi.suggestMeetingResources(event, date, false);

      expect(api.post.mock.calls[0][1].offices).toEqual([
        {
          id: 5,
          filter: resourcesFilter,
          selectedResourceEmails: [resourceEmail]
        }
      ]);
    });
  });
});
