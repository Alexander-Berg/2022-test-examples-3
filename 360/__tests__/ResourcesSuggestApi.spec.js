import moment from 'moment';

import ResourcesSuggestApi from '../ResourcesSuggestApi';

describe('ResourcesSuggestApi', () => {
  describe('getSuggestions', () => {
    test('должен отправлять запрос на получение предложений', () => {
      const api = {
        post: jest.fn()
      };
      const resourcesSuggestApi = new ResourcesSuggestApi(api);

      resourcesSuggestApi.getSuggestions({
        query: 'Авр',
        officeId: 2,
        filter: '',
        start: '2018-01-01T10:30:00',
        end: '2018-01-01T11:00:00',
        repetition: {},
        exceptEventId: 100,
        instanceStartTs: Number(moment('2018-01-01T07:00:00'))
      });

      expect(api.post).toBeCalledWith('/find-available-resources', {
        query: 'Авр',
        officeId: 2,
        filter: '',
        start: '2018-01-01T10:30:00',
        end: '2018-01-01T11:00:00',
        repetition: {},
        exceptEventId: 100,
        instanceStartTs: '2018-01-01T07:00:00'
      });
    });

    test('не должен отправлять instanceStartTs, если его не передали', () => {
      const api = {
        post: jest.fn()
      };
      const resourcesSuggestApi = new ResourcesSuggestApi(api);

      resourcesSuggestApi.getSuggestions({
        query: 'Авр',
        officeId: 2,
        filter: '',
        start: '2018-01-01T10:30:00',
        end: '2018-01-01T11:00:00',
        repetition: {},
        exceptEventId: 100
      });

      expect(api.post).toBeCalledWith('/find-available-resources', {
        query: 'Авр',
        officeId: 2,
        filter: '',
        start: '2018-01-01T10:30:00',
        end: '2018-01-01T11:00:00',
        repetition: {},
        exceptEventId: 100
      });
    });
  });

  describe('getSuggestionsDetails', () => {
    test('должен отправлять запрос c joinенными email', () => {
      const api = {
        post: jest.fn()
      };
      const resourcesSuggestApi = new ResourcesSuggestApi(api);

      const suggestions = [
        {
          email: 'cherdak_simf@yandex-team.ru'
        },
        {
          email: 'prachechnaya_simf@yandex-team.ru'
        }
      ];

      resourcesSuggestApi.getSuggestionsDetails({suggestions});

      expect(api.post).toBeCalledWith('/find-users-and-resources', {
        loginOrEmails: suggestions.map(item => item.email).join(',')
      });
    });
  });
});
