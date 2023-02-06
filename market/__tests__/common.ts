import { Word } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getSafeWordName } from 'src/utils/common';

const words: Word[] = [
  {
    lang_id: 253,
    name: 'foo',
  },
  {
    lang_id: 254,
    name: 'bar',
  },
  {
    lang_id: 225,
    name: 'baz',
  },
];

describe('src/utils/common', () => {
  describe('getSafeWordName', () => {
    test('должен вернуть найденную строку', () => {
      expect(getSafeWordName(words)).toEqual('baz');
      expect(getSafeWordName(words, 253)).toEqual('foo');
      expect(getSafeWordName(words, 254)).toEqual('bar');
      expect(getSafeWordName(words, 225)).toEqual('baz');
    });

    test('должен вернуть первое значение в массиве', () => {
      expect(getSafeWordName(words, 256)).toEqual('foo');
    });

    test('должен вернуть пустую строку', () => {
      expect(getSafeWordName()).toEqual('');
      expect(getSafeWordName([])).toEqual('');
      expect(getSafeWordName([], 254)).toEqual('');
    });
  });
});
