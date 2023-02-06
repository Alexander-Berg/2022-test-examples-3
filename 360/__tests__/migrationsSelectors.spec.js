import * as environment from 'configs/environment';

import {migrationsVersionSelector} from '../migrationsSelectors';

jest.mock('../manifest', () => {
  return {
    latestVersion: 'testLatestVersion'
  };
});

describe('migrationsSelectors', () => {
  describe('migrationsVersionSelector', () => {
    test('если это оффлайн календарь в моб приложении, то должен брать версию из стейта', () => {
      sinon.stub(environment, 'isStaticMobileApp').value(true);
      expect(migrationsVersionSelector({migrations: {version: 'stateVersion'}})).toBe(
        'stateVersion'
      );
    });

    test('если это не оффлайн календарь, то должен брать самую последнюю версию', () => {
      expect(migrationsVersionSelector({migrations: {version: 'stateVersion'}})).toBe(
        'testLatestVersion'
      );
    });

    test('если нет стейта с миграцией вернуть null', () => {
      sinon.stub(environment, 'isStaticMobileApp').value(true);
      expect(migrationsVersionSelector({})).toBe(null);
    });
  });
});
