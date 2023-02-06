import React from 'react';
import { render } from '@testing-library/react';
import { FullFormalizationsInfo } from './index';
import { getHypothesisFromSource } from './utils';
import { shopModel, formalizationValues } from 'src/test/data/shopModel';
import { categoryData, parameter } from 'src/test/data/categoryData';
import { MarketParameterValue, ShopModelView } from 'src/java/definitions';

// отображены ли текст по которому были выведены значения для marketValues
const hasDisplayHypothesis = (
  getByText: (text: string) => HTMLElement,
  model: ShopModelView,
  rule: MarketParameterValue
) => {
  const hypothesisSource = getHypothesisFromSource(model, rule.valPos!);
  getByText(hypothesisSource);
};

describe('FullFormalizationsInfo', () => {
  test('render FullFormalizationsInfo', () => {
    const firstRule = formalizationValues[0];
    const secondRule = formalizationValues[1];

    const { getByText } = render(
      <FullFormalizationsInfo
        model={shopModel}
        values={formalizationValues}
        parameter={parameter}
        categoryData={categoryData}
      />
    );

    hasDisplayHypothesis(getByText, shopModel, firstRule);
    hasDisplayHypothesis(getByText, shopModel, secondRule);

    // есть ли значения из marketValues
    getByText(firstRule.value.hypothesis!);
    getByText(secondRule.value.hypothesis!);
  });
});
