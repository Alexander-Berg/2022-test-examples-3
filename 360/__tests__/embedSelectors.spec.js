import {Map, List} from 'immutable';

import {isEmbed, getLayers, getPrivateToken, getEmbedTitle} from '../embedSelectors';
import {EMBED_WEEK_PATHNAME} from '../embedConstants';

describe('embedSelectors', () => {
  describe('isEmbed', () => {
    test('должен определять, находимся ли на роуте embed', () => {
      expect(isEmbed.resultFunc({pathname: EMBED_WEEK_PATHNAME})).toBe(true);
    });
  });

  describe('getLayers', () => {
    test('должен возвращать пользовательские слои в виде List', () => {
      const layer1 = {id: 101, name: 'name_1'};
      const layer2 = {id: 102, name: 'name_2'};
      const layers = new Map({
        101: layer1,
        102: layer2
      });
      const expectedResult = new List([layer1, layer2]);

      expect(getLayers.resultFunc(layers)).toEqual(expectedResult);
    });
  });

  describe('getPrivateToken', () => {
    test('должен возвращать private_token из урла', () => {
      const private_token = 'sdb298hi1knkdjnakm321m';
      const search = `?pewpew=1&private_token=${private_token}`;
      const location = {search};

      expect(getPrivateToken.resultFunc(location)).toBe(private_token);
    });
  });

  describe('getEmbedTitle', () => {
    test('должен возвращать список имён слоёв через запятую, если в стейте более двух слоёв', () => {
      const layer1 = new Map({id: 101, name: 'name_1'});
      const layer2 = new Map({id: 102, name: 'name_2'});
      const layers = new List([layer1, layer2]);

      expect(getEmbedTitle.resultFunc(layers)).toBe('name_1, name_2');
    });

    test('должен имя первого слоя, если в стейте один слой', () => {
      const name = 'Pew pew';
      const layer = new Map({name});
      const layers = new List([layer]);

      expect(getEmbedTitle.resultFunc(layers)).toBe(name);
    });
  });
});
