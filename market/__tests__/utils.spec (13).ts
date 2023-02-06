import {
  formator,
  carNumberWithoutAnySymbols,
  isValidCarNumber,
} from '@/screens/CarNumberScreen/utils';

describe('src/screens/CarNumberScreen/utils', () => {
  it('formator, возвращет возвращает корректное значение ', () => {
    expect(formator('P 182 6 | 5')).toEqual('P 182');
    expect(formator('182')).toEqual('');
    expect(formator('182AA')).toEqual('');
    expect(formator('A18223')).toEqual('A 182');
    expect(formator('A182 }{ 23')).toEqual('A 182');
    expect(formator('Q123 / ww 23')).toEqual('');
    expect(formator('A182 /Я 7')).toEqual('A 182');
    expect(formator('A182 /А 7')).toEqual('A 182 А');
    expect(formator('A182 А О7')).toEqual('A 182 АО | 7');
    expect(formator('A182ААА О7')).toEqual('A 182 АА | 7');
    expect(formator('B 182 АА 777')).toEqual('B 182 АА | 777');
    expect(formator('b 182 aa 777')).toEqual('B 182 AA | 777');
    expect(formator('baa777')).toEqual('B 7');
    expect(formator('baaa777')).toEqual('B');
    expect(formator('b77766666')).toEqual('B 777');
    expect(formator('b77@%$$#&*!**@&')).toEqual('B 77');
    expect(formator('b7@%$$#&*!**@&7aa77b')).toEqual('B 77');
    expect(formator('b7@%$$#&*!**@&77a77b77777')).toEqual('B 777 A');
  });

  it('carNumberWithoutAnySymbols, возвращет корректное значение 1', () => {
    expect(carNumberWithoutAnySymbols('b7@%$$#&*!**@&77a77b77777')).toEqual(
      'b777a77b77777',
    );
    expect(carNumberWithoutAnySymbols('b 182 aa 777')).toEqual('b182aa777');
    expect(carNumberWithoutAnySymbols('')).toEqual('');
    expect(carNumberWithoutAnySymbols('B 182 AA 777')).toEqual('B182AA777');
    expect(carNumberWithoutAnySymbols('Z 182ЯЯ7')).toEqual('1827');
  });

  it('carNumberWithoutAnySymbols, возвращет корректное значение 2', () => {
    expect(isValidCarNumber('B 182 AA 777')).toBeTruthy();
    expect(isValidCarNumber('B182AA777')).toBeTruthy();
    expect(isValidCarNumber('Z 182 A 000')).toEqual(false);
    expect(isValidCarNumber('Z 182ЯЯ7')).toEqual(false);
    expect(isValidCarNumber('P 182 6 | 5')).toEqual(false);
  });
});
