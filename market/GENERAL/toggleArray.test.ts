import { toggleArray } from './toggleArray';

describe('toggleArray', () => {
  it('works', () => {
    expect(toggleArray([], 'a')).toEqual(['a']);
    expect(toggleArray(['a'], 'a')).toEqual([]);
  });
});
