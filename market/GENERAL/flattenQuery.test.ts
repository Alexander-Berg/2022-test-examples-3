import { flattenQuery } from './flattenQuery';

describe('flatendQuery', () => {
  it('should return an empty object if an empty object was passed to input', () => {
    expect(flattenQuery({})).toEqual({});
  });

  it('should collapse nested objects into a simple object', () => {
    expect(flattenQuery({ a: { b: { c: 1 } } })).toEqual({ 'a.b.c': 1 });
  });

  it('should collapse array into a simple object with indexed keys', () => {
    expect(flattenQuery({ a: { b: [{ c: 1 }, { d: '2' }] } })).toEqual({ 'a.b[0].c': 1, 'a.b[1].d': '2' });
  });

  it('null should be unchanged', () => {
    expect(flattenQuery({ a: { b: null } })).toEqual({ 'a.b': null });
  });
});
