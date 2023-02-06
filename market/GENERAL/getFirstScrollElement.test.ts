import { getFirstScrollElement } from './getFirstScrollElement';

describe('libs/popup', () => {
  describe('dom-utils/getFirstScrollElement', () => {
    it('should returns itself if it is scroll element', () => {
      const el = document.createElement('div');
      el.style.overflow = 'scroll';

      expect(getFirstScrollElement(el)).toBe(el);
    });

    it('should returns first parent scroll element', () => {
      const expected = document.createElement('div');
      const parent = document.createElement('div');
      const el = document.createElement('div');

      expected.style.overflow = 'scroll';
      expected.appendChild(parent);
      parent.appendChild(el);

      expect(getFirstScrollElement(el)).toBe(expected);
    });

    it('should returns null if the scroll element does not exist', () => {
      const el = document.createElement('div');

      expect(getFirstScrollElement(el)).toBe(null);
    });

    it('should returns null if a non HTMLElement is passed', () => {
      expect(getFirstScrollElement(document)).toBe(null);
    });
  });
});
