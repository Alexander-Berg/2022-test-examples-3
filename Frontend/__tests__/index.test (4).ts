import * as path from 'path';
import { Serializer } from '../lib/Serializer';

const snapshotTests = [
  { name: 'флаг с указанием нескольких политик', file: '__data__/white-list.ts' },
  { name: 'не новостной флаг и флаг бекенда', file: '__data__/non-news-frontend-flag.ts' },
];

const processWorkingDirectory = process.env.PWD;

if (processWorkingDirectory === undefined) {
  throw new Error('process.env.PWD is undefined');
}

describe('Корректно сериализуется в json:', () => {
  snapshotTests.forEach(({ name, file }) => {
    test(name, () => {
      const serializer = new Serializer(processWorkingDirectory, path.join(__dirname, file));
      const result = serializer.getFlags();

      expect(result).toMatchSnapshot('не совпадает снепшот');
    });
  });
});

const illegalFlagTests = [
  {
    name: 'union случайных строк',
    file: '__data__/no-literals-in-union.ts',
    expectedError: 'Для значения в union был использован не входящий в enum элемент',
  },
  {
    name: 'union строки и enum',
    file: '__data__/no-literals-in-union-with-enum.ts',
    expectedError: 'Для значения в union был использован не входящий в enum элемент',
  },
  {
    name: 'случайную строку в шаблонной строке',
    file: '__data__/no-literals.ts',
    expectedError: 'Некорректно задано значение для шаблонной строки',
  },
  {
    name: 'перечисление с неизвестной стратегией',
    file: '__data__/no-enum-without-policies.ts',
    expectedError: 'неизвестное перечисление, добавьте стратегию в template-parser',
  },
  {
    name: 'объединение нескольких перечислений',
    file: '__data__/no-several-enums-union.ts',
    expectedError: 'Значения в union могут быть элементы только из одного enum',
  },
  {
    name: 'использование чисел',
    file: '__data__/no-numbers.ts',
    expectedError: 'Некорректно задано значение для шаблонной строки',
  },
];

describe('Невозможно сериализовать (ожидаем ошибку):', () => {
  illegalFlagTests.forEach(({ name, file, expectedError }) => {
    test(name, () => {
      expect(() => {
        const serializer = new Serializer(processWorkingDirectory, path.join(__dirname, file));

        serializer.getFlags();
      }).toThrow(expectedError);
    });
  });
});
