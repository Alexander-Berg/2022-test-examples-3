import {
  filterOptionsByLabel,
  getOptionByNumberValue,
  getOptionByValue,
  getOptionFromBoolean,
  getOptionFromString,
  getValueFromSingleOption,
} from './Select.utils';
import { ANY_OPTION, FALSE_OPTION, TRUE_OPTION } from 'src/components/Select/constants';

const getTestOptions = (count: number) =>
  Array.from(Array(count).keys()).map((v, index) => ({ label: `test${index}`, value: `test${index}` }));

describe('Select utils', () => {
  it('filterOptions', () => {
    expect(filterOptionsByLabel()).toEqual([]);

    const options = getTestOptions(150);

    expect(filterOptionsByLabel(options)).toHaveLength(100);

    expect(filterOptionsByLabel(options, 'test')).toHaveLength(100);
    expect(filterOptionsByLabel(options, '12')).toHaveLength(12);
  });
  it('getOptionFromString', () => {
    expect(getOptionFromString('test')).toEqual({ value: 'test', label: 'test' });
  });
  it('getOptionFromBoolean', () => {
    expect(getOptionFromBoolean(true)).toEqual(TRUE_OPTION);
    expect(getOptionFromBoolean(false)).toEqual(FALSE_OPTION);
    expect(getOptionFromBoolean(undefined)).toEqual(ANY_OPTION);
  });
  it('getOptionByValue', () => {
    expect(getOptionByValue(getTestOptions(4), 'test2')).toEqual({ value: 'test2', label: 'test2' });
  });
  it('getOptionByValue', () => {
    expect(
      getOptionByNumberValue(
        [
          { value: '1', label: 'test1' },
          { value: '2', label: 'test2' },
        ],
        2
      )
    ).toEqual({
      value: '2',
      label: 'test2',
    });
  });
  it('getValueFromSingleOption', () => {
    expect(getValueFromSingleOption({ value: 'test2', label: 'test2' })).toEqual('test2');
    expect(getValueFromSingleOption(null)).toEqual(undefined);
  });
});
