import React, { FC } from 'react';

import { useDescribeMapping, setupWithReatom } from 'src/test/withReatom';
import { MarketHeader, MarketHeaderProps } from './MarketHeader';
import { parameter, simpleMapping } from 'src/test/data';
import { UiParamMapping } from 'src/utils/types';

const TestApp: FC<MarketHeaderProps & { mappings: UiParamMapping[] }> = ({ mappings, ...props }) => {
  useDescribeMapping(mappings);

  return <MarketHeader {...props} />;
};

const mappings = [
  {
    ...simpleMapping,
    id: 1,
    shopParams: [
      {
        name: 'Комментарий (для категорийщика)',
      },
    ],
  },
  {
    ...simpleMapping,
    id: 2,
    shopParams: [
      {
        name: '#Торговая марка (ассортимент)',
      },
    ],
  },
];

describe('MarketHeader', () => {
  test('Отображение информации о параметре', () => {
    const { app } = setupWithReatom(<TestApp parameter={parameter} mappings={mappings} />);
    app.getByText('Комментарий (для категорийщика), #Торговая марка (ассортимент)');
    app.getByText(parameter.name);
  });
});
