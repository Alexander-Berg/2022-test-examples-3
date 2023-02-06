import {getFavoriteContacts} from '../abookSuggestSelectors';

describe('abookSuggestSelectors', () => {
  describe('getFavoriteContacts', () => {
    test('должен возвращать список избранных контактов', () => {
      const favoriteContacts = ['contact1', 'contact2'];
      const state = {abookSuggest: {favoriteContacts}};

      expect(getFavoriteContacts(state)).toEqual(favoriteContacts);
    });
  });
});
