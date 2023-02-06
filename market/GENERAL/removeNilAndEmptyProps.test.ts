import { removeNilAndEmptyProps } from './removeNilAndEmptyProps';

describe('utils', () => {
  describe('removeNilAndEmptyProps', () => {
    it('should be removed an undefined values', () => {
      expect(removeNilAndEmptyProps({ a: undefined, b: 'foo' })).toEqual({ b: 'foo' });
    });

    it('should be removed a null values', () => {
      expect(removeNilAndEmptyProps({ a: null, b: 'foo' })).toEqual({ b: 'foo' });
    });

    it('should be removed an empty string values', () => {
      expect(removeNilAndEmptyProps({ a: '', b: 'foo' })).toEqual({ b: 'foo' });
    });

    it('should be removed an empty array values', () => {
      expect(removeNilAndEmptyProps({ a: [], b: 'foo' })).toEqual({ b: 'foo' });
    });

    it('should be skiped a non empty values', () => {
      expect(removeNilAndEmptyProps({ a: false, b: 0 })).toEqual({ a: false, b: 0 });
    });
  });
});
