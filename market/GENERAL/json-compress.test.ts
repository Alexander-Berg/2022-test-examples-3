import { compress, decompress } from './json-compress';

describe('json-compress', () => {
  it('Should compress simple', () => {
    expect(decompress(compress(true))).toEqual(true);
    expect(decompress(compress(false))).toEqual(false);
    expect(decompress(compress(1))).toEqual(1);
  });

  it('Should process strings', () => {
    expect(decompress(compress('a'))).toEqual('a');
  });

  it('Should process objects', () => {
    expect(decompress(compress({ a: 1 }))).toEqual({ a: 1 });
  });

  it('Should process undefined', () => {
    expect(decompress(compress(undefined))).toEqual(undefined);
  });

  it('Should process undefined values', () => {
    expect(decompress(compress({ a: undefined }))).toEqual({ a: undefined });
  });

  it('Should process objects with numeric keys', () => {
    expect(decompress(compress({ 1: 1 }))).toEqual({ 1: 1 });
  });
  it('Should compress arrays', () => {
    const compressed = compress([{ a: 1 }]);
    expect(decompress(compressed)).toEqual([{ a: 1 }]);
    expect(compressed.keys).toHaveLength(1);
  });

  it('Should give sort keys', () => {
    const value = [
      { a: 0, some: 1 },
      { some: 2, other: 3 },
    ];
    const compressed = compress(value);
    expect(decompress(compressed)).toEqual(value);
    expect(compressed.keys).toEqual(['some', 'a', 'other']);
  });
});
