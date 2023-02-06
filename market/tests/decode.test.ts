import { decode, IQueryConfig, QueryTypes } from '..';

interface ICommonParams {
  str?: string;
  num?: number;
  bool?: boolean;
  arr?: number[];
  json?: object;
}

interface IRequiredParams {
  required: string;
}

const emptyConfig: IQueryConfig<{}> = {};

const commonConfig: IQueryConfig<ICommonParams> = {
  str: {
    type: QueryTypes.string,
  },
  num: {
    type: QueryTypes.number,
  },
  bool: {
    type: QueryTypes.boolean,
  },
  arr: {
    type: QueryTypes.arrayOf(QueryTypes.number),
  },
  json: {
    type: QueryTypes.json,
  },
};

const configWithRequiredParams: IQueryConfig<IRequiredParams> = {
  required: {
    type: QueryTypes.string,
    defaultValue: 'foo',
  },
};

describe('decode', () => {
  it('should return empty object', () => {
    expect(decode(emptyConfig, {})).toEqual({});
    expect(decode(commonConfig, {})).toEqual({});
    expect(decode(commonConfig, { foo: 'some' })).toEqual({});
  });

  it('should return default values', () => {
    expect(decode(configWithRequiredParams, {})).toEqual({ required: 'foo' });
    expect(decode(configWithRequiredParams, { foo: undefined })).toEqual({ required: 'foo' });
    expect(decode(configWithRequiredParams, { foo: null })).toEqual({ required: 'foo' });
    expect(
      decode(
        {
          required: {
            type: QueryTypes.string,
            defaultValue: () => 'some',
          },
        },
        { foo: null }
      )
    ).toEqual({ required: 'some' });
  });

  it('should return typed values', () => {
    expect(
      decode(commonConfig, {
        str: 'some',
        num: '100',
        bool: '0',
        arr: ['0', '1', '2', '3'],
        json: '{"valid": true}',
      })
    ).toEqual({
      str: 'some',
      num: 100,
      bool: false,
      arr: [0, 1, 2, 3],
      json: { valid: true },
    });
  });
});
