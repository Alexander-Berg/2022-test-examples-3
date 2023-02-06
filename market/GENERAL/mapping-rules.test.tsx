import { getEditedRules, approveRules } from './mapping-rules';
import { parameter, simpleMapping, categoryData } from 'src/test/data';
import { MarketParamValue, ParamMappingRule } from 'src/java/definitions';

const defaultRule = {
  deleted: false,
  hypothesis: false,
  id: 57189,
  paramMappingId: 25991,
  shopValues: { 'Торговая марка': 'suncraft' },
};

const ruleKey = 'Торговая марка:suncraft';

const getTestRules = (
  marketValues: MarketParamValue[],
  rule?: Partial<ParamMappingRule>
): Record<string, ParamMappingRule> => {
  return {
    [ruleKey]: {
      ...defaultRule,
      ...rule,
      marketValues: {
        [parameter.id]: marketValues,
      },
    },
  };
};

const mappingWithHypotheses = {
  ...simpleMapping,
  rules: {
    'Торговая марка:suncraft': {
      ...defaultRule,
      hypothesis: true,
      id: 57189,
      shopValues: { 'Торговая марка': 'suncraft' },
      marketValues: {
        [parameter.id]: [{ hypothesis: 'suncraft', optionId: 10389099 }],
      },
    },
    'Торговая марка:samsung': {
      deleted: false,
      hypothesis: true,
      id: 57190,
      paramMappingId: 25991,
      shopValues: { 'Торговая марка': 'samsung' },
      marketValues: {
        [parameter.id]: [{ hypothesis: 'samsung', optionId: 10389099 }],
      },
    },
  },
};

describe('mapping-rules', () => {
  test('approved only filtered rule', () => {
    const approved = approveRules(mappingWithHypotheses, categoryData, 'samsung');
    expect(approved.rules?.['Торговая марка:suncraft'].hypothesis).toBeTruthy();
    // только у отфильтрованого правила должан подтвердится гипотеза
    expect(approved.rules?.['Торговая марка:samsung'].hypothesis).toBeFalsy();
  });

  test('approved all rules', () => {
    const approved = approveRules(mappingWithHypotheses, categoryData);
    // все правила гипотезы должны подтвердится
    expect(approved.rules?.['Торговая марка:suncraft'].hypothesis).toBeFalsy();
    expect(approved.rules?.['Торговая марка:samsung'].hypothesis).toBeFalsy();
  });

  getEditedRulesTestCases().forEach(el => {
    test(`getEditedRules -> ${el.test}`, () => {
      const { deleteRule, addRules } = getEditedRules(parameter, el.origRules, el.editedRules);

      expect(deleteRule.length).toBe(el.expectDeleted.length);
      expect(deleteRule[0]).toBe(el.expectDeleted[0]);

      expect(addRules.length).toBe(el.expectAdded);
    });
  });
});

function getEditedRulesTestCases() {
  return [
    {
      test: 'add and delete rule',
      origRules: getTestRules([{ hypothesis: '4Home', optionId: 10389098 }]),
      editedRules: getTestRules([{ hypothesis: '555', optionId: 10389099 }]),
      expectDeleted: [57189],
      expectAdded: 1,
    },
    {
      test: 'delete rule',
      origRules: getTestRules([{ hypothesis: '4Home', optionId: 10389098 }]),
      editedRules: getTestRules([]),
      expectDeleted: [57189],
      expectAdded: 0,
    },
    {
      test: 'add rule',
      editedRules: getTestRules([{ hypothesis: '555', optionId: 10389099 }]),
      expectDeleted: [],
      expectAdded: 1,
    },
    {
      test: 'same rule',
      editedRules: getTestRules([{ hypothesis: '555', optionId: 10389099 }]),
      origRules: getTestRules([{ hypothesis: '555', optionId: 10389099 }]),
      expectDeleted: [],
      expectAdded: 0,
    },
  ];
}
