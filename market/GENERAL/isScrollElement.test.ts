import { isScrollElement } from './isScrollElement';

describe('libs/popup', () => {
  describe('dom-utils/isScrollElement', () => {
    it('should return true if the overflow is auto', () => {
      const element = document.createElement('div');
      element.style.overflow = 'auto';

      expect(isScrollElement(element)).toBe(true);
    });

    it('should return true if the overflow is scroll', () => {
      const element = document.createElement('div');
      element.style.overflow = 'scroll';

      expect(isScrollElement(element)).toBe(true);
    });

    it('should return true if the overflow is overlay', () => {
      const element = document.createElement('div');
      element.style.overflow = 'overlay';

      expect(isScrollElement(element)).toBe(true);
    });

    it('should return true if the overflow is hidden', () => {
      const element = document.createElement('div');
      element.style.overflow = 'hidden';

      expect(isScrollElement(element)).toBe(true);
    });

    it('should return true if an overflowX exists', () => {
      const element = document.createElement('div');
      element.style.overflowX = 'auto';

      expect(isScrollElement(element)).toBe(true);
    });

    it('should return true if an overflowY exists', () => {
      const element = document.createElement('div');
      element.style.overflowY = 'auto';

      expect(isScrollElement(element)).toBe(true);
    });

    it('should return false if an overflow not set', () => {
      const element = document.createElement('div');

      expect(isScrollElement(element)).toBe(false);
    });
  });
});
