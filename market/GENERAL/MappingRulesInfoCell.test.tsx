import React from 'react';
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { MappingRulesInfoCell } from './MappingRulesInfoCell';
import { simpleMappingWithRule, simpleMapping, shopModel, categoryData, rule, ruleKey } from 'src/test/data';
import { ParamMappingType } from 'src/java/definitions';
import { ParamMappingRow } from '../../types';
import { setupWithReatom } from 'src/test/withReatom';
import { categoryDataAtom, setCategoryData } from 'src/store/categories';
import { filterAtom, setFilterAction } from 'src/pages/ParameterSetting/store/filter.atom';
import { shopModelsAtom, setAllShopModelsAction } from 'src/store/shopModels';
import {
  paramMappingsAtom,
  updateParamMappingsAction,
} from 'src/pages/ParameterSetting/store/mappings/paramMappings.atom';

const mapping = {
  ...simpleMappingWithRule,
  displayShopParams: '',
  displayMarketParams: '',
  original: simpleMappingWithRule,
};

const mappingWithHypothesis = {
  ...mapping,
  rules: { [ruleKey]: { ...rule, hypothesis: true } },
};

const forceMapping = {
  ...mapping,
  rules: {},
  mappingType: ParamMappingType.FORCE_MAPPING,
};

const atoms = { categoryDataAtom, shopModelsAtom, filterAtom, paramMappingsAtom };
const dispatches = [
  setAllShopModelsAction([shopModel]),
  setCategoryData(categoryData),
  setFilterAction({ marketCategoryId: categoryData.hid }),
];

describe('MappingRulesInfoCell', () => {
  const openRule = jest.fn();

  test('render with rules', async () => {
    const { app } = setupWithReatom(<MappingRulesInfoCell mapping={mapping} openRule={openRule} />, atoms, dispatches);

    await waitFor(() => {
      const btn = app.getByText('1 правило');
      userEvent.click(btn);
    });

    expect(openRule.mock.calls.length).toBe(1);

    jest.resetAllMocks();
  });

  test('render with hypotheses', () => {
    const { app } = setupWithReatom(
      <MappingRulesInfoCell mapping={mappingWithHypothesis} openRule={openRule} />,
      atoms,
      dispatches
    );
    app.getByText('1 гипотеза');
  });

  test('render with auto rule', async () => {
    const { app } = setupWithReatom(
      <MappingRulesInfoCell mapping={forceMapping} openRule={openRule} />,
      atoms,
      dispatches
    );

    await waitFor(() => {
      app.getByText('1 автоправило');
    });
  });

  test('direct mapping', () => {
    const onRules = jest.fn();
    const directMapping = {
      ...simpleMappingWithRule,
      mappingType: ParamMappingType.DIRECT,
    } as ParamMappingRow;

    const { app } = setupWithReatom(
      <MappingRulesInfoCell mapping={directMapping} openRule={onRules} />,
      atoms,
      dispatches
    );
    app.getByText(/Значения будут скопированы/i);
  });

  test('main image mapping', () => {
    const onRules = jest.fn();
    const imageMapping = {
      ...simpleMappingWithRule,
      mappingType: ParamMappingType.FIRST_PICTURE,
    } as ParamMappingRow;
    const { app } = setupWithReatom(
      <MappingRulesInfoCell mapping={imageMapping} openRule={onRules} />,
      atoms,
      dispatches
    );
    app.getByText(/Значения будут скопированы/i);
  });

  test('additional image mapping', () => {
    const onRule = jest.fn();
    const additionalImageMapping = {
      ...simpleMappingWithRule,
      mappingType: ParamMappingType.PICTURE,
    } as ParamMappingRow;

    const { app } = setupWithReatom(
      <MappingRulesInfoCell mapping={additionalImageMapping} openRule={onRule} />,
      atoms,
      dispatches
    );
    app.getByText(/Значения будут скопированы/i);
  });

  test('mapping without rule', () => {
    const onRule = jest.fn();

    const additionalImageMapping = {
      ...simpleMappingWithRule,
      mappingType: ParamMappingType.PICTURE,
    } as ParamMappingRow;

    const { app } = setupWithReatom(
      <MappingRulesInfoCell mapping={additionalImageMapping} openRule={onRule} />,
      atoms,
      dispatches
    );
    app.getByText(/Значения будут скопированы/i);
  });

  test('ignore rules for current category', () => {
    const onRule = jest.fn();
    const withoutRuleMapping = {
      ...simpleMapping,
      mappingType: ParamMappingType.MAPPING,
      original: simpleMapping,
    } as ParamMappingRow;

    const categoryMapping = { ...withoutRuleMapping, categoryId: categoryData.hid, id: 1 };

    const { app } = setupWithReatom(<MappingRulesInfoCell mapping={withoutRuleMapping} openRule={onRule} />, atoms, [
      ...dispatches,
      setCategoryData({ ...categoryData, leaf: true }),
      updateParamMappingsAction([withoutRuleMapping, categoryMapping]),
    ]);

    app.getByText(/Игнорируются/i);
  });
});
