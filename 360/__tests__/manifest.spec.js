import migrationsManifest from '../manifest';

describe('migration manifest', () => {
  test('first migration', () => {
    const state = {a: 1};
    expect(migrationsManifest[1](state)).toEqual({a: 1});
  });
});
