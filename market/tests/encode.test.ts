import { encode, IQueryConfig, QueryTypes } from '..';

describe('encode', () => {
  it('should correctly encode the values', () => {
    const config: IQueryConfig<{ a?: string; b?: number; c?: boolean; d?: object; e?: number[] }> = {
      a: {
        type: QueryTypes.string,
      },
      b: {
        type: QueryTypes.number,
      },
      c: {
        type: QueryTypes.boolean,
      },
      d: {
        type: QueryTypes.json,
      },
      e: {
        type: QueryTypes.arrayOf(QueryTypes.number),
      },
    };

    expect(encode(config, { a: 'foo', b: 1, c: false, d: { some: 'value' }, e: [0, 1, 2] })).toEqual({
      a: 'foo',
      b: '1',
      c: '0',
      d: '{"some":"value"}',
      e: ['0', '1', '2'],
    });
  });
});
