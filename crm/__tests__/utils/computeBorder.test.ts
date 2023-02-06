import { computeBorder } from '../../Group.utils';

describe('computeBorder', () => {
  describe('when index is last', () => {
    it('returns none', () => {
      expect(
        computeBorder({
          length: 6,
          index: 5,
        }),
      ).toBe('none');
    });
  });

  describe(`when index isn't last`, () => {
    it('returns undefined', () => {
      expect(
        computeBorder({
          length: 5,
          index: 3,
        }),
      ).toBe(undefined);
    });
  });
});
