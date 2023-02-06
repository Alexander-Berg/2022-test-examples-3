import React from 'react';
import { shallow } from 'enzyme';
import { assoc, __ } from 'ramda';

import { FormattersProps, ReplenishmentDataGridColumn } from 'src/pages/replenishment/components';
import {
  getRowFormatterCumulativeSales,
  getRowFormatterNormalSales,
  RowFormatter,
  RowFormatterForecast,
  RowFormatterIndex,
  RowFormatterPrice,
  RowFormatterSafetyStock,
  RowFormatterStatus,
  RowFormatterStock,
  RowFormatterStockCover,
  RowFormatterStockCoverBackward,
  RowFormatterVolume,
  RowFormatterWeight,
  RowFormatterSum,
  RowFormatterRecommend,
  RowFormatterSalePromoPeriod,
  RowFormatterPurchasePromoPeriod,
  RowFormatterHiddenInSummary,
  RowFormatterCorrectionReason,
  RowFormatterWarehouseStockCoverBackward,
  RowFormatterWarehouseStockCoverForward,
  RowFormatterWarehouseStockCoverBackwardTo,
  RowFormatterWarehouseStockCoverForwardTo,
  RowFormatterDate,
  RowFormatterAllVolume,
  RowFormatterCopyValue,
  RowFormatterQuantity,
} from './formatters';
import { NotProcessed } from './icons';

interface RowsMock {
  id?: number;
  warehouseId?: number | null;
  indexNum?: number;
  needsManualReview?: boolean;
  weight?: number;
  width?: number;
  length?: number;
  height?: number;
  sales1p?: any[] | null;
  salesAll?: any[] | null;
  stockCoverBackward?: number;
  stockCoverForward?: number;
}

type FormatterTest = React.FC<FormattersProps<RowsMock, any>>;

const rowMock: RowsMock = {
  id: 1,
  warehouseId: 1337,
  indexNum: -2,
};

const column: ReplenishmentDataGridColumn<RowsMock> = {
  name: 'Column',
  width: 300,
};

const formatterMock: FormattersProps<RowsMock, any> = {
  key: 'warehouseId',
  value: '',
  row: rowMock,
  column,
  index: 0,
};

const customMock = assoc('row', __, formatterMock);

describe('<RecommendationsGridTable /> formatters', () => {
  it('RowFormatter', () => {
    const Formatter = RowFormatter as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} value="hello" />).html()).toBe('<div title="hello">hello</div>');
    expect(shallow(<Formatter {...formatterMock} value={null} />).html()).toBe('<div title="null">—</div>');
    expect(shallow(<Formatter {...formatterMock} value="" />).html()).toBe('<div></div>');
  });

  it('RowFormatterIndex', () => {
    const Formatter = RowFormatterIndex as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} value={22} />).html()).toBe('<div>2</div>');
    expect(shallow(<Formatter {...{ ...formatterMock, row: {} }} value={22} />).html()).toBe('<div>23</div>');
  });

  it('RowFormatterStatus', () => {
    const Formatter = RowFormatterStatus as FormatterTest;
    expect(
      shallow(<Formatter {...{ ...formatterMock, row: { needsManualReview: true } }} />).find(NotProcessed)
    ).toBeDefined();
    expect(
      shallow(<Formatter {...{ ...formatterMock, row: { needsManualReview: false } }} />).find(NotProcessed)
    ).toBeDefined();
  });

  it('RowFormatterWeight', () => {
    const Formatter = RowFormatterWeight as FormatterTest;
    expect(shallow(<Formatter {...{ ...formatterMock, row: { weight: 5 } }} />).html()).toBe('<div>0.005</div>');
  });

  it('RowFormatterVolume', () => {
    const Formatter = RowFormatterVolume as FormatterTest;
    expect(shallow(<Formatter {...{ ...formatterMock, row: { width: 5, length: 5, height: 5 } }} />).html()).toBe(
      '<div>125</div>'
    );
  });

  it('getRowFormatterCumulativeSales', () => {
    const sales1p = [1, 2, 3, 4, 5, 6, 7, 8];
    const sales1p_short = [1, 2, 3, 4, 5];
    const salesAll: number[] | null = [1, 2, 3, 4, 5, 6, 7, 8];
    let Formatter;

    expect(() => getRowFormatterCumulativeSales(0)).toThrow('Requested week number (0) is below minimum');

    expect(() => getRowFormatterCumulativeSales(9)).toThrow('Requested week number (9) is above maximum');

    Formatter = getRowFormatterCumulativeSales(1) as FormatterTest;
    expect(shallow(<Formatter {...{ ...formatterMock, row: { sales1p: [], salesAll: [] } }} />).html()).toBe(
      '<div>-/-</div>'
    );
    expect(shallow(<Formatter {...{ ...formatterMock, row: { sales1p, salesAll: null } }} />).html()).toBe(
      '<div>-/-</div>'
    );
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: { sales1p, salesAll },
          }}
        />
      ).html()
    ).toBe('<div>1/1</div>');

    Formatter = getRowFormatterCumulativeSales(2) as FormatterTest;
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: { sales1p, salesAll },
          }}
        />
      ).html()
    ).toBe('<div>2/2</div>');

    Formatter = getRowFormatterCumulativeSales(5) as FormatterTest;
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: { sales1p, salesAll },
          }}
        />
      ).html()
    ).toBe('<div>5/5</div>');

    Formatter = getRowFormatterCumulativeSales(8) as FormatterTest;
    expect(shallow(<Formatter {...{ ...formatterMock, row: { sales1p: sales1p_short, salesAll } }} />).html()).toBe(
      '<div>5/5</div>'
    );
    Formatter = getRowFormatterCumulativeSales(6) as FormatterTest;
    expect(
      shallow(<Formatter {...{ ...formatterMock, row: { sales1p: sales1p_short, salesAll: sales1p_short } }} />).html()
    ).toBe('<div>-/-</div>');
  });

  it('getRowFormatterNormalSales', () => {
    const sales1p = [1, 2, 3, 4, 5, 6, 7, 8];
    const salesAll: number[] | null = [1, 2, 3, 4, 5, 6, 7, 8];
    let Formatter;

    expect(() => getRowFormatterNormalSales(0)).toThrow('Requested week number (0) is below minimum');

    expect(() => getRowFormatterNormalSales(9)).toThrow('Requested week number (9) is above maximum');

    Formatter = getRowFormatterNormalSales(1) as FormatterTest;
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: { sales1p, salesAll },
          }}
        />
      ).html()
    ).toBe('<div>1/1</div>');

    Formatter = getRowFormatterNormalSales(2) as FormatterTest;
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: { sales1p, salesAll },
          }}
        />
      ).html()
    ).toBe('<div>1/1</div>');

    expect(shallow(<Formatter {...{ ...formatterMock, row: { sales1p: [], salesAll: [] } }} />).html()).toBe(
      '<div>-/-</div>'
    );
    expect(shallow(<Formatter {...{ ...formatterMock, row: { sales1p, salesAll: null } }} />).html()).toBe(
      '<div>-/-</div>'
    );

    Formatter = getRowFormatterNormalSales(5) as FormatterTest;
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: { sales1p, salesAll },
          }}
        />
      ).html()
    ).toBe('<div>1/1</div>');

    Formatter = getRowFormatterNormalSales(4) as FormatterTest;
    expect(
      shallow(
        <Formatter
          {...{ ...formatterMock, row: { sales1p: sales1p.map(() => NaN), salesAll: salesAll.map(() => NaN) } }}
        />
      ).html()
    ).toBe('<div>-/-</div>');

    Formatter = getRowFormatterNormalSales(5) as FormatterTest;
    expect(
      shallow(
        <Formatter {...{ ...formatterMock, row: { sales1p: sales1p.slice(0, 5), salesAll: salesAll.slice(0, 5) } }} />
      ).html()
    ).toBe('<div>-/-</div>');
  });

  it('RowFormatterStockCoverBackward', () => {
    const Formatter = RowFormatterStockCoverBackward as FormatterTest;

    // Null SCB
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="—/—">—/—</div>');

    // Existing SCB in summary or normal row
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              stockCoverBackward: 222,
            },
          }}
        />
      ).html()
    ).toBe('<div title="—/222">—/222</div>');

    // Existing SCB in subrow
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              ...rowMock,
              stockCoverBackward: 222,
            },
          }}
        />
      ).html()
    ).toBe('<div title="—/—">—/—</div>');

    // Zero SCB
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              stockCoverBackward: 0,
            },
          }}
        />
      ).html()
    ).toBe('<div title="—/—">—/—</div>');
  });

  it('RowFormatterStockCover', () => {
    // Null SCF
    const Formatter = RowFormatterStockCover as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="0">—</div>');

    // Existing SCF in summary or normal row
    expect(shallow(<Formatter {...customMock({})} value={295} />).html()).toBe('<div title="295">295</div>');

    // Existing SCF in subrow
    expect(shallow(<Formatter {...formatterMock} value={295} />).html()).toBe('<div title="0">—</div>');

    // Zero SCF
    expect(shallow(<Formatter {...formatterMock} value={0} />).html()).toBe('<div title="0">—</div>');
  });

  it('RowFormatterStock', () => {
    const Formatter = RowFormatterStock() as FormatterTest;
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              stock: 5,
              stockOverall: 5,
            },
          }}
        />
      ).html()
    ).toBe('<div>5/5</div>');
  });

  it('RowFormatterForecast', () => {
    const Formatter = RowFormatterForecast as FormatterTest;
    const props = {
      ...formatterMock,
      row: { id: -1 },
    };
    const props2 = {
      ...formatterMock,
      row: { id: 1, indexNum: -1 },
    };
    const props3 = {
      ...formatterMock,
      row: { id: 1, indexNum: 1 },
    };
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div>—</div>');
    expect(shallow(<Formatter {...props} value={5} />).html()).toBe('<div title="5.0">5.0</div>');
    expect(shallow(<Formatter {...props} value={10} />).html()).toBe('<div title="10">10</div>');
    expect(shallow(<Formatter {...props2} value={10} />).html()).toBe('<div>—</div>');
    expect(shallow(<Formatter {...props3} value={10} />).html()).toBe('<div title="10">10</div>');
    expect(shallow(<Formatter {...props} />).html()).toBe('<div></div>');
    expect(shallow(<Formatter {...formatterMock} value={5} />).html()).toBe('<div>—</div>');
  });

  it('RowFormatterSafetyStock', () => {
    const Formatter = RowFormatterSafetyStock as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe(
      '<div title="Минимальный неснижаемый остаток (шт): Не задан\n' +
        'Минимальный неснижаемый остаток (дни): Не задан\n' +
        'Максимальный неснижаемый остаток (шт): Не задан\n' +
        'Максимальный неснижаемый остаток (дни): Не задан"></div>'
    );

    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              minItems: 2,
              minItemsByDays: 1,
              maxItems: 3,
              maxItemsByDays: 4,
            },
          }}
        />
      ).html()
    ).toBe(
      '<div title="Минимальный неснижаемый остаток (шт): 2.00 шт\n' +
        'Минимальный неснижаемый остаток (дни): 1.00 шт\n' +
        'Максимальный неснижаемый остаток (шт): 3.00 шт\n' +
        'Максимальный неснижаемый остаток (дни): 4.00 шт" class="warning"></div>'
    );
  });

  it('RowFormatterPrice', () => {
    const Formatter = RowFormatterPrice as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div>—</div>');
    expect(shallow(<Formatter {...formatterMock} value={1234562.4} />).html()).toBe('<div>1 234 562.40</div>');
  });

  it('RowFormatterSum', () => {
    const Formatter = RowFormatterSum as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div>—</div>');
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              purchaseResultPrice: 2,
              setQuantity: 12345667.2,
            },
          }}
        />
      ).html()
    ).toBe('<div>24 691 334.40</div>');
  });

  it('RowFormatterPurchasePromoPeriod', () => {
    const Formatter = RowFormatterPurchasePromoPeriod as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div>—</div>');
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              purchasePromoStart: '2020-10-10',
              purchasePromoEnd: '2020-11-10',
            },
          }}
        />
      ).html()
    ).toBe('<div>10.10-10.11</div>');
  });

  it('RowFormatterSalePromoPeriod', () => {
    const Formatter = RowFormatterSalePromoPeriod as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div>—</div>');

    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              salePromoStart: '2020-10-10',
              salePromoEnd: '2020-11-10',
            },
          }}
        />
      ).html()
    ).toBe('<div>10.10-10.11</div>');
  });

  it('RowFormatterRecommend', () => {
    const Formatter = RowFormatterRecommend as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="undefined"></div>');

    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              hasCheaperRecommendation: false,
              purchaseQuantity: 123,
            },
          }}
        />
      ).html()
    ).toBe('<div title="123">123</div>');
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              hasCheaperRecommendation: true,
              purchaseQuantity: 123,
            },
          }}
        />
      ).html()
    ).toBe(
      '<div title="Есть рекомендация на закупку по меньшей цене от другого поставщика" style="color:#ce2029"><b>123</b></div>'
    );
  });

  it('RowFormatterHiddenInSummary', () => {
    const Formatter = RowFormatterHiddenInSummary as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div><div>—</div></div>');

    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            row: {
              id: -1,
            },
            value: 13,
          }}
        />
      ).html()
    ).toBe('<div><div>—</div></div>');
    expect(
      shallow(
        <Formatter
          {...{
            ...formatterMock,
            value: 13,
          }}
        />
      ).html()
    ).toBe('<div title="13"><div>13</div></div>');
  });

  it('RowFormatterCorrectionReason', () => {
    const Formatter = RowFormatterCorrectionReason as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="—"><div>—</div></div>');
    expect(shallow(<Formatter {...formatterMock} value={null} />).html()).toBe('<div title="—"><div>—</div></div>');
    expect(shallow(<Formatter {...formatterMock} value={{ id: 1, name: 'test test' }} />).html()).toBe(
      '<div title="test test"><div>test test</div></div>'
    );
    expect(shallow(<Formatter {...formatterMock} value={{ id: 1, name: '' }} />).html()).toBe(
      '<div title="—"><div>—</div></div>'
    );
  });

  it('RowFormatterWarehouseStockCoverBackward', () => {
    const Formatter = RowFormatterWarehouseStockCoverBackward as FormatterTest;

    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="0">—</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            stockFrom: 5,
            transitFrom: 6,
            setQuantity: 7,
            orders28dOverall: 8,
            orders56dOverall: 9,
          })}
        />
      ).html()
    ).toBe('<div title="63">63</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            stockFrom: 5,
            transitFrom: 6,
            setQuantity: 7,
            orders28dOverall: null,
            orders56dOverall: 9,
          })}
        />
      ).html()
    ).toBe('<div title="112">112</div>');
  });

  it('RowFormatterWarehouseStockCoverForward', () => {
    const Formatter = RowFormatterWarehouseStockCoverForward as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="0">—</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            stockFrom: 5,
            transitFrom: 6,
            setQuantity: 7,
            salesForecast28d: 8,
            salesForecast56d: 9,
          })}
        />
      ).html()
    ).toBe('<div title="63">63</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            stockFrom: 5,
            transitFrom: 6,
            setQuantity: 7,
            salesForecast28d: null,
            salesForecast56d: 9,
          })}
        />
      ).html()
    ).toBe('<div title="112">112</div>');
  });

  it('RowFormatterWarehouseStockCoverBackwardTo', () => {
    const Formatter = RowFormatterWarehouseStockCoverBackwardTo as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="0">—</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            stockTo: 5,
            transitTo: 6,
            setQuantity: 7,
            orders28dDestinationWarehouseRegions: 8,
            orders56dDestinationWarehouseRegions: 9,
          })}
        />
      ).html()
    ).toBe('<div title="63">63</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            stockTo: 5,
            transitTo: 6,
            setQuantity: 7,
            orders28dDestinationWarehouseRegions: null,
            orders56dDestinationWarehouseRegions: 9,
          })}
        />
      ).html()
    ).toBe('<div title="112">112</div>');
  });

  it('RowFormatterWarehouseStockCoverForwardTo', () => {
    const Formatter = RowFormatterWarehouseStockCoverForwardTo as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="0">—</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            stockTo: 5,
            transitTo: 6,
            setQuantity: 7,
            salesForecast28d: 8,
            salesForecast56d: 9,
          })}
        />
      ).html()
    ).toBe('<div title="63">63</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            stockTo: 5,
            transitTo: 6,
            setQuantity: 7,
            salesForecast28d: null,
            salesForecast56d: 9,
          })}
        />
      ).html()
    ).toBe('<div title="112">112</div>');
  });

  it('RowFormatterDate', () => {
    const Formatter = RowFormatterDate as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div title="-">-</div>');
    expect(shallow(<Formatter {...formatterMock} value="2020-10-10" />).html()).toBe(
      '<div title="10.10.2020">10.10.2020</div>'
    );
  });
  it('RowFormatterAllVolume', () => {
    const Formatter = RowFormatterAllVolume as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} />).html()).toBe('<div>NaN</div>');
    expect(
      shallow(
        <Formatter
          {...customMock({
            length: 2,
            width: 3,
            height: 4,
            setQuantity: 5,
            purchaseQuantity: 6,
          })}
        />
      ).html()
    ).toBe('<div>120</div>');
  });

  it('RowFormatterCopyValue', () => {
    const Formatter = RowFormatterCopyValue as FormatterTest;
    expect(shallow(<Formatter {...formatterMock} value="hello" />).text()).toBe('hello<CopyButton />');
    expect(shallow(<Formatter {...formatterMock} value={null} />).text()).toBe('—');
  });

  it('RowFormatterQuantity', () => {
    const Formatter = RowFormatterQuantity as FormatterTest;
    expect(
      shallow(<Formatter {...{ ...customMock({ setQuantity: 5, purchaseQuantity: 6 }), value: 5 }} />).html()
    ).toBe('<div title="5">5</div>');

    expect(
      shallow(<Formatter {...{ ...customMock({ setQuantity: 5, purchaseQuantity: 55 }), value: 5 }} />).html()
    ).toBe('<div title="Значение сильно отличается от рекомендованного" class="warning">5</div>');
  });
});
