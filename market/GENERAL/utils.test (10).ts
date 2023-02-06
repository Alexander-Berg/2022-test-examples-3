import { dateFilter, getSortFunc, isServiceFilter, textFilter } from './utils';
import { ETableSortableField } from './types';
import { IPreparedParamValue } from 'src/entities/skuInfo/types';

describe('sku info utils', () => {
  it('sortByDate', () => {
    const a = { modification_date: 1234567 } as IPreparedParamValue;
    const b = { modification_date: 9876543 } as IPreparedParamValue;
    const c = { modification_date: 9999999 } as IPreparedParamValue;

    let sortFunc = getSortFunc(ETableSortableField.DATE, 'DESC');
    expect([a, b, c].sort(sortFunc)).toEqual([c, b, a]);

    sortFunc = getSortFunc(ETableSortableField.DATE, 'ASC');
    expect([a, b, c].sort(sortFunc)).toEqual([a, b, c]);
  });

  it('sortByTitle', () => {
    const a = { title: 'Хоп' } as IPreparedParamValue;
    const b = { title: 'хей' } as IPreparedParamValue;
    const c = { title: 'La la lei' } as IPreparedParamValue;

    let sortFunc = getSortFunc(ETableSortableField.TITLE, 'ASC');
    expect([a, b, c].sort(sortFunc)).toEqual([c, b, a]);

    sortFunc = getSortFunc(ETableSortableField.TITLE, 'DESC');
    expect([a, b, c].sort(sortFunc)).toEqual([a, b, c]);
  });

  it('sortById', () => {
    const a = { id: 1 } as IPreparedParamValue;
    const b = { id: -1 } as IPreparedParamValue;
    const c = { id: 9000 } as IPreparedParamValue;

    let sortFunc = getSortFunc(ETableSortableField.ID, 'ASC');
    expect([a, b, c].sort(sortFunc)).toEqual([b, a, c]);

    sortFunc = getSortFunc(ETableSortableField.ID, 'DESC');
    expect([a, b, c].sort(sortFunc)).toEqual([c, a, b]);
  });

  it('sortByXslName', () => {
    const a = { xsl_name: undefined } as IPreparedParamValue;
    const b = { xsl_name: undefined } as IPreparedParamValue;
    const c = { xsl_name: 'Bruce Wayne' } as IPreparedParamValue;
    const d = { xsl_name: '' } as IPreparedParamValue;

    let sortFunc = getSortFunc(ETableSortableField.XSLNAME, 'ASC');
    expect([a, b, c, d].sort(sortFunc)).toEqual([a, b, d, c]);

    sortFunc = getSortFunc(ETableSortableField.XSLNAME, 'DESC');
    expect([a, b, c, d].sort(sortFunc)).toEqual([c, a, b, d]);
  });

  it.each([
    [true, true, true],
    [false, true, true],
    [false, false, true],
    [true, false, false],
    [undefined, false, true],
    [undefined, true, true],
  ])('isServiceFilter(%s, %s) equals %s', (isService, showService, result) => {
    expect(isServiceFilter({ isService } as IPreparedParamValue, showService)).toEqual(result);
  });

  it.each([
    [undefined, null, null, true],
    [123, null, null, true],
    [123, null, new Date(124), true],
    [123, null, new Date(122), false],
    [123, new Date(124), null, false],
    [123, new Date(122), null, true],
    [123, new Date(122), new Date(124), true],
    [123, new Date(122), new Date(122), false],
    [123, new Date(124), new Date(124), false],
  ])(
    'dateFilter(%s, %s, %s) equals $s',
    (ts: number | undefined, dateFrom: Date | null, dateTo: Date | null, result: boolean) => {
      expect(dateFilter({ modification_date: ts } as IPreparedParamValue, dateFrom, dateTo)).toEqual(result);
    }
  );

  it.each([
    [undefined, undefined, undefined, undefined, true],
    ['xsl', undefined, undefined, undefined, true],
    [undefined, 'title', undefined, undefined, true],
    [undefined, undefined, 0, undefined, true],
    ['testikovich', undefined, undefined, 'testik', true],
    ['testikovich', undefined, -1, '-1', true],
    [undefined, 'undefined', -1, 'defi', true],
    [undefined, 'undefined', -1, 'not-defined', false],
    [undefined, undefined, undefined, 'not-defined', false],
  ])(
    'textFilter(%s, %s, %s, %s) equals $s',
    (
      xsl_name: string | undefined,
      title: string | undefined,
      param_id: number | undefined,
      searchText: string | undefined,
      result: boolean
    ) => {
      expect(textFilter({ xsl_name, title, param_id } as IPreparedParamValue, searchText)).toEqual(result);
    }
  );
});
