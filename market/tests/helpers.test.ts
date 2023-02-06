import { createHelperType } from '..';

describe('helpers', () => {
  describe('createHelperType', () => {
    it('should return an object with fields of type', () => {
      const encode = (): string => '';
      const decode = (): string => '';
      const type = createHelperType({ encode, decode });

      expect(type()).toEqual({
        type: {
          encode,
          decode,
        },
      });

      expect(type('default value')).toEqual({
        type: {
          encode,
          decode,
        },
        defaultValue: 'default value',
      });
    });
  });
});
