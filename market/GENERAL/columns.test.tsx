import { shallow } from 'enzyme';
import React from 'react';

import { createRecommendation } from 'src/test/data/recomendations';
import { DemandType } from 'src/java/definitions-replenishment';
import { getDirectColumns } from './columns';
import { FormattersProps } from 'src/pages/replenishment/components';

const recommendationWithSales = createRecommendation({
  sales1p: [1, 2, 3, 4, 5, 6, 7, 8],
  salesAll: [11, 12, 13, 14, 15, 16, 17, 18],
});

const weeklySales1p = [1, 1, 1, 1, 1, 1, 1, 1];
const weeklySalesAll = [11, 1, 1, 1, 1, 1, 1, 1];

interface RowsMosk {
  sales1p: number[];
  salesAll: number[];

  [key: string]: any;
}

const rowMock: FormattersProps<RowsMosk, any> = {
  value: '',
  row: recommendationWithSales,
  column: {
    name: 'Column',
    width: 300,
  },
  key: 'warehouseId',
  index: 0,
};

describe('<RecommendationsGridTable /> direct columns', () => {
  it('should properly generate 1P columns with cumulative sales', () => {
    const columns = getDirectColumns({
      correctionReasons: [],
      useCumulativeSales: true,
      demandType: DemandType.TYPE_1P,
    });

    expect(columns.orders7days.name).toStrictEqual('Продажи за 1 неделю 1P/все');
    expect(columns.orders14days.name).toStrictEqual('Продажи за 2 недели 1P/все');
    expect(columns.orders21days.name).toStrictEqual('Продажи за 3 недели 1P/все');
    expect(columns.orders28days.name).toStrictEqual('Продажи за 4 недели 1P/все');
    expect(columns.orders35days.name).toStrictEqual('Продажи за 5 недель 1P/все');
    expect(columns.orders42days.name).toStrictEqual('Продажи за 6 недель 1P/все');
    expect(columns.orders49days.name).toStrictEqual('Продажи за 7 недель 1P/все');
    expect(columns.orders56days.name).toStrictEqual('Продажи за 8 недель 1P/все');

    const { sales1p, salesAll } = recommendationWithSales;

    for (let i = 1; i < 9; i++) {
      const Formatter = columns[`orders${i * 7}days`].formatter! as React.FC<FormattersProps<RowsMosk, any>>;
      expect(shallow(<Formatter {...rowMock} />).html()).toStrictEqual(
        `<div>${sales1p[i - 1]}/${salesAll[i - 1]}</div>`
      );
    }
  });

  it('should properly generate 1P columns with per-week sales', () => {
    const columns = getDirectColumns({
      correctionReasons: [],
      useCumulativeSales: false,
      demandType: DemandType.TYPE_1P,
    });

    expect(columns.orders7days.name).toStrictEqual('Продажи за 1-ю неделю 1P/все');
    expect(columns.orders14days.name).toStrictEqual('Продажи за 2-ю неделю 1P/все');
    expect(columns.orders21days.name).toStrictEqual('Продажи за 3-ю неделю 1P/все');
    expect(columns.orders28days.name).toStrictEqual('Продажи за 4-ю неделю 1P/все');
    expect(columns.orders35days.name).toStrictEqual('Продажи за 5-ю неделю 1P/все');
    expect(columns.orders42days.name).toStrictEqual('Продажи за 6-ю неделю 1P/все');
    expect(columns.orders49days.name).toStrictEqual('Продажи за 7-ю неделю 1P/все');
    expect(columns.orders56days.name).toStrictEqual('Продажи за 8-ю неделю 1P/все');

    for (let i = 1; i < 9; i++) {
      const Formatter = columns[`orders${i * 7}days`].formatter! as React.FC<FormattersProps<RowsMosk, any>>;
      expect(shallow(<Formatter {...rowMock} />).html()).toStrictEqual(
        `<div>${weeklySales1p[i - 1]}/${weeklySalesAll[i - 1]}</div>`
      );
    }
  });

  it('should properly generate 3P columns with cumulative sales', () => {
    const columns = getDirectColumns({
      correctionReasons: [],
      useCumulativeSales: true,
      demandType: DemandType.TYPE_3P,
    });

    expect(columns.orders7days.name).toStrictEqual('Продажи за 1 неделю 3P/все');
    expect(columns.orders14days.name).toStrictEqual('Продажи за 2 недели 3P/все');
    expect(columns.orders21days.name).toStrictEqual('Продажи за 3 недели 3P/все');
    expect(columns.orders28days.name).toStrictEqual('Продажи за 4 недели 3P/все');
    expect(columns.orders35days.name).toStrictEqual('Продажи за 5 недель 3P/все');
    expect(columns.orders42days.name).toStrictEqual('Продажи за 6 недель 3P/все');
    expect(columns.orders49days.name).toStrictEqual('Продажи за 7 недель 3P/все');
    expect(columns.orders56days.name).toStrictEqual('Продажи за 8 недель 3P/все');

    const { sales1p, salesAll } = recommendationWithSales;

    for (let i = 1; i < 9; i++) {
      const Formatter = columns[`orders${i * 7}days`].formatter! as React.FC<FormattersProps<RowsMosk, any>>;
      expect(shallow(<Formatter {...rowMock} />).html()).toStrictEqual(
        `<div>${sales1p[i - 1]}/${salesAll[i - 1]}</div>`
      );
    }
  });

  it('should properly generate 3P columns with per-week sales', () => {
    const columns = getDirectColumns({
      correctionReasons: [],
      useCumulativeSales: false,
      demandType: DemandType.TYPE_3P,
    });

    expect(columns.orders7days.name).toStrictEqual('Продажи за 1-ю неделю 3P/все');
    expect(columns.orders14days.name).toStrictEqual('Продажи за 2-ю неделю 3P/все');
    expect(columns.orders21days.name).toStrictEqual('Продажи за 3-ю неделю 3P/все');
    expect(columns.orders28days.name).toStrictEqual('Продажи за 4-ю неделю 3P/все');
    expect(columns.orders35days.name).toStrictEqual('Продажи за 5-ю неделю 3P/все');
    expect(columns.orders42days.name).toStrictEqual('Продажи за 6-ю неделю 3P/все');
    expect(columns.orders49days.name).toStrictEqual('Продажи за 7-ю неделю 3P/все');
    expect(columns.orders56days.name).toStrictEqual('Продажи за 8-ю неделю 3P/все');

    for (let i = 1; i < 9; i++) {
      const Formatter = columns[`orders${i * 7}days`].formatter! as React.FC<FormattersProps<RowsMosk, any>>;
      expect(shallow(<Formatter {...rowMock} />).html()).toStrictEqual(
        `<div>${weeklySales1p[i - 1]}/${weeklySalesAll[i - 1]}</div>`
      );
    }
  });
});
