import migrationsVersionSetter from '../migrationsVersionSetter';

describe('migrationsVersionSetter', () => {
  test('должен правильно проставить версию миграции в стейт', () => {
    expect(migrationsVersionSetter({otherState: {}}, 20)).toEqual({
      otherState: {},
      migrations: {version: 20}
    });
  });
});
