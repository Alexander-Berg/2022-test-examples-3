import { createQueryConverter, QueryTypes } from '..';

describe('createQueryConverter', () => {
  it('should return encode and decode functions', () => {
    const converter = createQueryConverter();

    expect(typeof converter.encode).toBe('function');
    expect(typeof converter.decode).toBe('function');
  });

  it('should return same object', () => {
    const converter = createQueryConverter();

    const obj = { foo: 'bar' };

    expect(converter.encode(obj)).toBe(obj);
    expect(converter.decode(obj)).toBe(obj);
  });

  it('should correctly encode and decode', () => {
    const converter = createQueryConverter<{ a?: number }>({
      a: {
        type: QueryTypes.number,
      },
    });

    expect(converter.encode({ a: 300 })).toEqual({ a: '300' });
    expect(converter.decode({ foo: 'bar', a: '100' })).toEqual({ a: 100 });
  });
});
