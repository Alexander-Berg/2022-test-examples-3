import React from 'react';
import { render } from '@testing-library/react';
import { FormalizationInfo } from './FormalizationInfo';
import { getHypothesisFromSource } from './utils';
import { shopModel, formalizationValues } from 'src/test/data/shopModel';
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

describe('FormalizationInfo', () => {
  test('render FormalizationInfo', () => {
    const firstRule = formalizationValues[0];
    const { getByText } = render(<FormalizationInfo model={shopModel} value={firstRule} />);
    hasDisplayHypothesis(getByText, shopModel, firstRule);
  });
});
