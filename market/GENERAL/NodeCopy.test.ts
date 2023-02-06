import { normalizePath, matchPath } from './NodeCopy.utils';

describe('NodeCopyPlugin', () => {
  describe('utils', () => {
    describe('normalizePath', () => {
      it('should handle the valid paths', () => {
        expect(normalizePath('FIELD')).toEqual(['*', 'FIELD']);
        expect(normalizePath('FIELD1/FIELD2')).toEqual(['*', 'FIELD1', '*', 'FIELD2']);
        expect(normalizePath('[NODE]/FIELD')).toEqual(['*', '*', 'NODE', 'FIELD']);
        expect(normalizePath('FIELD1[NODE]/FIELD2')).toEqual(['*', 'FIELD1', 'NODE', 'FIELD2']);
      });

      it('should throw an error if the last segment contains a node', () => {
        expect(() => normalizePath('[NODE]')).toThrow();
      });

      it('should throw an error if the path is empty', () => {
        expect(() => normalizePath('')).toThrow();
      });

      it('should throw an error if the path is invalid', () => {
        expect(() => normalizePath('[]')).toThrow();
      });
    });

    describe('matchPath', () => {
      it('should handle wildcard segments', () => {
        expect(matchPath(['N1', 'F1'], ['*', '*'])).toBe(true);
        expect(matchPath(['N1', 'F1'], ['*', 'F1'])).toBe(true);
        expect(matchPath(['N1', 'F1'], ['N1', '*'])).toBe(true);
      });

      it('should match the end of the path', () => {
        expect(matchPath(['N1', 'F1', 'N2', 'F2', 'N3', 'F3'], ['N2', 'F2', 'N3', 'F3'])).toBe(true);
      });

      it('should return false if the target path is larger than the source path', () => {
        expect(matchPath(['N1', 'F1'], ['*', '*', 'N1', 'F1'])).toBe(false);
      });

      it('should return false if at least one segment does not match', () => {
        expect(matchPath(['N1', 'F1'], ['N1', 'F2'])).toBe(false);
        expect(matchPath(['N1', 'F1'], ['N2', 'F1'])).toBe(false);
        expect(matchPath(['N1', 'F1'], ['*', 'F2'])).toBe(false);
        expect(matchPath(['N1', 'F1'], ['N2', '*'])).toBe(false);
      });

      it('should return false if the target path is larger than the source path', () => {
        expect(matchPath(['N1', 'F1'], ['*', '*', 'N1', 'F1'])).toBe(false);
      });
    });
  });
});
