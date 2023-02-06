import SuggestMeetingTimesApi from '../SuggestMeetingTimesApi';

describe('SuggestMeetingTimesApi', () => {
  describe('suggestMeetingTimes', () => {
    test('должен отправлять запрос на получение рекомендаций мест и времени встречи', () => {
      const api = {
        post: jest.fn()
      };
      const suggestMeetingTimesApi = new SuggestMeetingTimesApi(api);
      const params = {
        mode: 'any-room',
        users: ['user1@yandex.ru'],
        offices: [
          {
            id: 2,
            filter: 'projector,medium',
            selectedResourceEmails: []
          }
        ],
        searchStart: '2018-01-30T00:00',
        searchBackward: false,
        eventStart: '2018-01-30T14:00',
        eventEnd: '2018-01-30T15:00',
        repetition: {},
        ignoreUsersEvents: false,
        numberOfOptions: 3,
        exceptEventId: 100,
        instanceStartTs: '2018-01-30T10:00'
      };

      suggestMeetingTimesApi.suggestMeetingTimes(params);

      expect(api.post).toBeCalledWith('/suggest-meeting-times', params);
    });
  });
});
