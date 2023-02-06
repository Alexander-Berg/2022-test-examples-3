import LocationSuggestApi from '../LocationSuggestApi';

describe('LocationSuggestApi', () => {
  describe('getSuggestions', () => {
    test('должен отправлять запрос на получение предложений', () => {
      const api = {
        post: jest.fn()
      };
      const locationSuggestApi = new LocationSuggestApi(api);

      locationSuggestApi.getSuggestions('St. P');

      expect(api.post).toBeCalledWith('/get-locations', {part: 'St. P'}, {cache: true});
    });
  });
});
