import React, { FC } from 'react';

import { getHypothesisFromSource } from '../FormalizationInfo/utils';
import { categoryData, parameter, modelWithFormalization, formalizationValues } from 'src/test/data';
import { MarketParameterValue, ShopModelView } from 'src/java/definitions';
import { ValuesInfo } from './ValuesInfo';
import { setupWithReatom, useDescribeMapping } from 'src/test/withReatom';
import { UiParamMapping } from 'src/utils/types';

// отображены ли текст по которому были выведены значения для marketValues
const hasDisplayHypothesis = (
  getByText: (text: string) => HTMLElement,
  model: ShopModelView,
  rule: MarketParameterValue
) => {
  const hypothesisSource = getHypothesisFromSource(model, rule.valPos!);
  getByText(hypothesisSource);
};

const TestApp: FC<{ model: ShopModelView; mapping?: UiParamMapping }> = ({ model, mapping }) => {
  useDescribeMapping(mapping ? [mapping] : []);

  return <ValuesInfo model={model} values={formalizationValues} parameter={parameter} categoryData={categoryData} />;
};

test('render ShortFormalizationInfo', () => {
  const firstRule = formalizationValues[0];
  const { app } = setupWithReatom(<TestApp model={modelWithFormalization} />);

  hasDisplayHypothesis(app.getByText, modelWithFormalization, firstRule);
  // отображается ли бейдж с кол-во значений отсавшейся формализации
  app.getByText(`+${formalizationValues.length}`);
});
