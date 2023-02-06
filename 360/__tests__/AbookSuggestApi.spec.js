import moment from 'moment';

import config from 'configs/config';

import AbookSuggestApi from '../AbookSuggestApi';

describe('AbookSuggestApi', () => {
  describe('getSuggestions', () => {
    test('должен отправлять запрос на получение предложений', () => {
      const api = {
        post: jest.fn()
      };
      const abookSuggestApi = new AbookSuggestApi(api);
      const user_type = 'common';

      sinon.stub(config.user, 'type').value(user_type);

      abookSuggestApi.getSuggestions({q: 'fre'});

      expect(api.post).toBeCalledWith('/suggest-contacts', {q: 'fre', user_type});
    });

    test('должен отправлять запрос на получение предложений с занятостью', () => {
      const api = {
        post: jest.fn()
      };
      const abookSuggestApi = new AbookSuggestApi(api);
      const user_type = 'common';

      sinon.stub(config.user, 'type').value(user_type);

      abookSuggestApi.getSuggestions({
        q: 'fre',
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00',
        instanceStartTs: Number(moment('2018-01-01T07:00:00'))
      });

      expect(api.post).toBeCalledWith('/suggest-contacts', {
        q: 'fre',
        user_type,
        availability: {
          start: '2018-01-01T10:00:00',
          end: '2018-01-01T11:00:00',
          instanceStartTs: '2018-01-01T07:00:00'
        }
      });
    });

    test('не должен отправлять instanceStartTs, если его не передали', () => {
      const api = {
        post: jest.fn()
      };
      const abookSuggestApi = new AbookSuggestApi(api);
      const user_type = 'common';

      sinon.stub(config.user, 'type').value(user_type);

      abookSuggestApi.getSuggestions({
        q: 'fre',
        start: '2018-01-01T10:00:00',
        end: '2018-01-01T11:00:00'
      });

      expect(api.post).toBeCalledWith('/suggest-contacts', {
        q: 'fre',
        user_type,
        availability: {
          start: '2018-01-01T10:00:00',
          end: '2018-01-01T11:00:00'
        }
      });
    });
  });

  describe('getFavoriteContacts', () => {
    test('должен отправлять запрос на получение популярных контактов', () => {
      const api = {
        post: jest.fn()
      };
      const abookSuggestApi = new AbookSuggestApi(api);

      abookSuggestApi.getFavoriteContacts();

      expect(api.post).toBeCalledWith(
        '/find-favorite-contacts',
        {},
        {
          cache: true
        }
      );
    });
  });
});
