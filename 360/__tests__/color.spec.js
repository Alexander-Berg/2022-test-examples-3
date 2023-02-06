import {darken, shade, tint, rgba, whiten} from 'utils/color';

describe('color', () => {
  describe('darken', () => {
    test('должен работать', () => {
      expect(darken('#16bae7', 10)).toBe('rgb(18, 148, 184)');
    });
  });

  describe('shade', () => {
    test('должен работать', () => {
      expect(shade('#16bae7', 10)).toBe('rgb(20, 167, 208)');
    });
  });

  describe('tint', () => {
    test('должен работать', () => {
      expect(tint('#16bae7', 10)).toBe('rgb(45, 193, 233)');
    });
  });

  describe('rgba', () => {
    test('должен работать', () => {
      expect(rgba('red', 0.5)).toBe('rgba(255, 0, 0, 0.5)');
    });
  });

  describe('whiten', () => {
    test('должен работать', () => {
      expect(whiten('rgba(0,255,0,.3)')).toBe('rgb(179, 255, 179)');
    });
  });
});
