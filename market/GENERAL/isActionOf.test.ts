import { declareAction } from '@reatom/core';

import { isActionOf } from './isActionOf';

describe('reatom-helpers/isActionOf', () => {
  const a1 = declareAction(['a1']);
  const a2 = declareAction(['a2']);
  const a3 = declareAction(['a3']);

  it('should work with single action-creator arg', () => {
    expect(isActionOf(a1)({ type: 'a1' })).toBe(true);
    expect(isActionOf(a1)({ type: 'a2' })).toBe(false);
  });

  it('should work with multiple action-creator args', () => {
    expect(isActionOf(a1, { type: 'a1' })).toBe(true);
    expect(isActionOf(a1, { type: 'a2' })).toBe(false);
  });

  it('should correctly assert for an array', () => {
    expect(isActionOf([a1, a2, a3], { type: 'a2' })).toBe(true);
    expect(isActionOf([a1, a2, a3], { type: 'a4' })).toBe(false);
  });
});
