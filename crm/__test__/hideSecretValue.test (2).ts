import { hideSecretValue } from '../hideSecretValue';

describe('hideSecretValue', () => {
  describe('when empty string', () => {
    it('returns empty string', () => {
      expect(hideSecretValue('')).toBe('');
    });
  });

  describe('when string length less 4', () => {
    it('returns XXX', () => {
      expect(hideSecretValue('abc')).toBe('XXX');
    });
  });

  describe('when string length more or equal 4', () => {
    it('returns {start}XXX{end}', () => {
      expect(hideSecretValue('1234567')).toBe('1XXX7');
    });
  });
});
