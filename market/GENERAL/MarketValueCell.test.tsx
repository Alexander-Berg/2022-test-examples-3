import React, { FC } from 'react';

import {
  shopModel,
  parameter,
  vendorParameter,
  formalizationValues,
  rules,
  simpleMapping,
  categoryData,
} from 'src/test/data';
import { MarketValueCell, MarketValueCellProps } from './MarketValueCell';
import { setupWithReatom, useDescribeMapping } from 'src/test/withReatom';

const formalizeValue = formalizationValues[0];
const ruleValue = rules[0];

const TestApp: FC<MarketValueCellProps> = props => {
  useDescribeMapping([simpleMapping]);
  return <MarketValueCell {...props} />;
};

describe('MarketValueCell', () => {
  test('formalize value', async () => {
    const { app } = setupWithReatom(
      <TestApp parameter={parameter} categoryData={categoryData} values={[formalizeValue]} model={shopModel} />
    );

    app.getByText(formalizeValue.value.hypothesis!);
  });

  test('rule value', async () => {
    const { app } = setupWithReatom(
      <TestApp parameter={vendorParameter} categoryData={categoryData} values={[ruleValue]} model={shopModel} />
    );

    app.getByText(ruleValue.value.hypothesis);
  });
});
