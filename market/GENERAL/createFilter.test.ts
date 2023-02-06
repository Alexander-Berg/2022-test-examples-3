import { createFilter } from './createFilter';

describe('utils', () => {
  describe('createFilter', () => {
    it('should filter without case sensitive', () => {
      const filter = createFilter({ ignoreCase: true });

      expect(filter(v => v, 'foo', ['foo', 'FOO', 'BAR'])).toEqual(['foo', 'FOO']);
    });

    it('should filter with case sensitive', () => {
      const filter = createFilter({ ignoreCase: false });

      expect(filter(v => v, 'O', ['foo', 'FOO', 'BAR'])).toEqual(['FOO']);
    });

    it('should filter without trim', () => {
      const filter = createFilter({ trim: false });

      expect(filter(v => v, ' foo ', ['foo', ' foo ', 'BAR'])).toEqual([' foo ']);
    });

    it('should filter with trim', () => {
      const filter = createFilter({ trim: true });

      expect(filter(v => v, ' foo ', ['foo', ' foo ', 'BAR'])).toEqual(['foo', ' foo ']);
    });

    it('should limit the number of elements', () => {
      const filter = createFilter({ limit: 2 });

      expect(filter(v => v, 'a', ['a1', 'a2', 'a3'])).toEqual(['a1', 'a2']);
    });

    it('should filter by property', () => {
      const filter = createFilter();

      expect(filter(v => v.v, 'a', [{ v: 'a' }, { v: 'b' }, { v: 'c' }])).toEqual([{ v: 'a' }]);
    });
  });
});
