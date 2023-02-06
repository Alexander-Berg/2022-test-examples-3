import {
  buildCommonParamValue,
  extractCommonParamValueData,
  getCommonEntityParamByName,
  toPlainOr,
} from './commonEntity';
import { navTreeTestFindResults, navTreeTestCommonEntityParam, navTreeTestMetadata } from './test-data';
import { CommonParam, CommonParamValueType } from '../java/definitions';

describe('extractCommonParamValueData', () => {
  it('should return nothing on a garbage input', () => {
    expect(extractCommonParamValueData()).toEqual(undefined);
  });
  it('should return value on valid input', () => {
    expect(
      extractCommonParamValueData(navTreeTestCommonEntityParam, navTreeTestMetadata.commonEntityType.commonParams)
    ).toEqual(0);
  });
});

describe('getCommonEntityParamByName', () => {
  it('should return nothing when param not found', () => {
    expect(getCommonEntityParamByName(navTreeTestFindResults.commonEntities[0], 'not-existing')).toEqual(undefined);
  });
  it('should return valid CommonParamValue', () => {
    expect(getCommonEntityParamByName(navTreeTestFindResults.commonEntities[0], 'tree_node_id')).toEqual(
      navTreeTestCommonEntityParam
    );
  });
});

describe('toPlainOr', () => {
  it('should return string', () => {
    expect(toPlainOr('str')).toEqual('str');
  });
  it('should return number', () => {
    expect(toPlainOr(123)).toEqual(123);
  });
  it('should return undefined on complex type', () => {
    expect(toPlainOr([])).toEqual(undefined);
  });
  it('should return fallback value on complex type', () => {
    expect(toPlainOr([], 'fallback value')).toEqual('fallback value');
  });
});

describe('buildCommonParamValue', () => {
  const numericParam = {
    commonParamName: 'numeric',
    commonParamValueType: CommonParamValueType.NUMERIC,
  };

  const int64Param = {
    commonParamName: 'int64Param',
    commonParamValueType: CommonParamValueType.INT64,
  };

  it.each([
    [123, [123]],
    ['', []],
    [undefined, []],
    [null, []],
    ['456', [456]],
    [[NaN, null, undefined], []],
    [
      ['1', '2', 'Peter Utin'],
      [1, 2],
    ],
  ])('numeric "%s" in commonParams = %s', (value: any, result) => {
    const commonParamValue = buildCommonParamValue(numericParam.commonParamName, value, [numericParam as CommonParam]);
    expect(commonParamValue?.numerics).toEqual(result);
  });

  it.each([
    [123, [123]],
    ['', []],
    [undefined, []],
    [null, []],
    ['456', [456]],
    [[NaN, null, undefined], []],
    [
      ['1', '2', 'Peter Utin'],
      [1, 2],
    ],
  ])('int64 "%s" in commonParams = %s', (value: any, result) => {
    const commonParamValue = buildCommonParamValue(int64Param.commonParamName, value, [int64Param as CommonParam]);
    expect(commonParamValue?.int64s).toEqual(result);
  });
});
