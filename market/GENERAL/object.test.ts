import { objectChanged } from 'src/utils/object';

describe('object utils', () => {
  it('provides correct names for russian multiples', () => {
    const obj = { a: 1, b: '2' };
    expect(objectChanged(obj, { a: 1, b: '2' })).toBeFalsy();
    expect(objectChanged(obj, { a: 1, b: '2', c: undefined })).toBeFalsy();
    expect(objectChanged({ ...obj, d: undefined }, { a: 1, b: '2' })).toBeFalsy();
    expect(objectChanged({ ...obj }, { a: 1, b: '2' })).toBeFalsy();

    expect(objectChanged(obj, { a: 1, b: '1' })).toBeTruthy();
    expect(objectChanged(obj, { a: 1, c: undefined })).toBeTruthy();
    expect(objectChanged({ ...obj, d: '2' }, { a: 1, b: '2' })).toBeTruthy();
    expect(objectChanged({ ...obj }, {})).toBeTruthy();
  });
});
