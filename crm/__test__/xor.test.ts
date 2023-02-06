import { xor } from '../xor';

describe('xor', () => {
  describe('when xor(true, true)', () => {
    it('returns false', () => {
      expect(xor(true, true)).toBe(false);
    });
  });

  describe('when xor(true, false)', () => {
    it('returns true', () => {
      expect(xor(true, false)).toBe(true);
    });
  });

  describe('when xor(false, true)', () => {
    it('returns true', () => {
      expect(xor(false, true)).toBe(true);
    });
  });

  describe('when xor(false, false)', () => {
    it('returns false', () => {
      expect(xor(false, false)).toBe(false);
    });
  });
});
