import { filterModelByParameterValue, getRegexpSafely, searchByText } from './utils';
import { MatchStringType } from './types';
import {
  shopModel,
  parameter,
  formalizationValues,
  categoryData,
  vendorRules,
  manualValues,
  vendorParameter,
} from 'src/test/data';
import { ValueSource } from 'src/java/definitions';

const round = 'круглый';
const halfRound = 'Полукруглый';

const regexpStr = '\\bround';
const sourceRegEx1 = 'round';
const sourceRegEx2 = 'halfround';

const filteredByParameterCases = [
  {
    name: 'by one source',
    model: {
      ...shopModel,
      marketValues: { [parameter.id]: formalizationValues },
    },
    filter: [{ parameterId: parameter.id, valueSource: [ValueSource.FORMALIZATION] }],
    expect: true,
  },
  {
    name: 'with diff source value',
    model: {
      ...shopModel,
      marketValues: {
        [parameter.id]: manualValues,
        [vendorParameter.id]: [vendorRules],
      },
    },
    filter: [
      { parameterId: parameter.id, valueSource: [ValueSource.MANUAL] },
      { parameterId: vendorParameter.id, valueSource: [ValueSource.RULE] },
    ],
    expect: true,
  },
  {
    name: 'search by value',
    model: {
      ...shopModel,
      marketValues: { [parameter.id]: manualValues },
    },
    filter: [{ parameterId: parameter.id, searchStr: 'king' }],
    expect: true,
  },
  {
    name: 'without parameter value',
    model: {
      ...shopModel,
      marketValues: { [parameter.id]: [] },
    },
    filter: [{ parameterId: parameter.id, withoutValue: true }],
    expect: true,
  },
  {
    name: 'with canceled value',
    model: {
      ...shopModel,
      marketValues: { [parameter.id]: [{ ...manualValues[0], value: { empty: true } }] },
    },
    filter: [{ parameterId: parameter.id, cancelValue: true }],
    expect: true,
  },
  {
    name: 'with rule source and cancel value',
    model: {
      ...shopModel,
      marketValues: { [parameter.id]: [{ ...manualValues[0], value: { empty: true } }] },
    },
    filter: [{ parameterId: parameter.id, cancelValue: false, valueSource: [ValueSource.MANUAL] }],
    expect: false,
  },
  {
    name: 'search by value, incorrect regexp',
    model: {
      ...shopModel,
      marketValues: { [parameter.id]: manualValues },
    },
    filter: [{ parameterId: parameter.id, searchStr: '/b**' }],
    expect: false,
  },
];

describe('filter utils', () => {
  test('Search exact match', () => {
    expect(searchByText(round, round, MatchStringType.FULL_WORLD)).toBeTruthy();
    expect(searchByText(halfRound, round, MatchStringType.FULL_WORLD)).toBeFalsy();
  });

  test('Search RegExp', () => {
    const regexp = new RegExp(regexpStr);
    expect(searchByText(sourceRegEx1, regexp)).toBeTruthy();
    expect(searchByText(sourceRegEx2, regexp)).toBeFalsy();
  });

  test('Search Include', () => {
    expect(searchByText(round, round, MatchStringType.INCLUDES)).toBeTruthy();
    expect(searchByText(halfRound, round, MatchStringType.INCLUDES)).toBeTruthy();
  });

  test('getRegexpSafely', () => {
    // eslint-disable-next-line no-useless-escape
    expect(getRegexpSafely(`/двуспальный\/`)).toEqual(/двуспальный/);
  });

  filteredByParameterCases.forEach(testCase => {
    test(`filter by parameter value case = ${testCase.name}`, () => {
      const filtered = filterModelByParameterValue(testCase.model, testCase.filter, categoryData);
      expect(filtered).toBe(testCase.expect);
    });
  });
});
