import { getQuotated } from '../getQuotated';

describe('getQuoted', () => {
  test('quotation', () => {
    expect(getQuotated('')).toEqual('');
    expect(getQuotated('   ')).toEqual('   ');
    expect(getQuotated('Joint-stock Company "Vector"')).toEqual('Joint-stock Company "Vector"');
    expect(getQuotated('ООО Вектор')).toEqual('«ООО Вектор»');
    expect(getQuotated('  ООО Вектор    ')).toEqual('«ООО Вектор»');
    expect(getQuotated('  "ООО Вектор" ')).toEqual('«ООО Вектор»');
    expect(getQuotated('«ООО Вектор»')).toEqual('«ООО Вектор»');
    expect(getQuotated('   «ООО Вектор» ')).toEqual('«ООО Вектор»');
    expect(getQuotated('"ООО" Вектор')).toEqual('«ООО» Вектор');
    expect(getQuotated('ООО "Вектор"')).toEqual('ООО «Вектор»');
    expect(getQuotated('«ООО» Вектор')).toEqual('«ООО» Вектор');
    expect(getQuotated('ООО «Вектор»')).toEqual('ООО «Вектор»');
    expect(getQuotated('"ООО "Вектор"')).toEqual('«ООО «Вектор»');
    expect(getQuotated('"ООО" Вектор"')).toEqual('«ООО» Вектор»');
    expect(getQuotated('«ООО» «Вектор»')).toEqual('«ООО» «Вектор»');
    expect(getQuotated('"ООО" "Вектор"')).toEqual('«ООО» «Вектор»');
    expect(getQuotated('"АБВ "ГДЕ" ЁЖЗ"')).toEqual('«АБВ «ГДЕ» ЁЖЗ»');
  });
});
