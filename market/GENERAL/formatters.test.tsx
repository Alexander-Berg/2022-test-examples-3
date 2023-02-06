import React from 'react';
import { shallow } from 'enzyme';
import { FormatterProps } from 'react-data-grid';

import {
  RowFormatter,
  RowFormatterIndex,
  RowFormatterDate,
  RowFormatterCorrectionReason,
  RowFormatterVolume,
  RowFormatterPrice,
  RowFormatterSum,
  RowFormatterStock,
  RowFormatterPurchasePromoPeriod,
  RowFormatterSalePromoPeriod,
} from './formatters';
import { correctionReasons } from 'src/test/data/correctionReasons';
import { createInterWarehouseRecommendationRow } from 'src/test/data/interWarehouseRecommendations';
import { createRecommendationRow } from 'src/test/data/recomendations';

const column = {
  name: 'Column',
  key: 'title',
  idx: 0,
  width: 300,
  left: 100,
};

const rowMock: FormatterProps<any> = {
  value: '',
  row: { warehouseId: 1337 },
  column,
  dependentValues: {},
  isScrolling: false,
  rowIdx: 23,
};

describe('<RecommendationsGridTable /> formatters', () => {
  it('RowFormatter', () => {
    const Formatter = RowFormatter;
    expect(shallow(<Formatter {...rowMock} value="hello" />).html()).toBe('<div title="hello">hello</div>');
  });

  it('RowFormatterIndex', () => {
    const Formatter = RowFormatterIndex;
    expect(shallow(<Formatter {...rowMock} value="hello" />).html()).toBe('<div>24</div>');
  });

  it('RowFormatterDate', () => {
    expect(() => {
      expect(shallow(<RowFormatterDate {...rowMock} value="2020-01-01" />).html()).toStrictEqual(
        '<div title="01.01.2020">01.01.2020</div>'
      );
    }).not.toThrow();
  });

  it('RowFormatterDate WITH EMPTY DATE', () => {
    expect(() => {
      expect(shallow(<RowFormatterDate {...rowMock} value="" />).html()).toStrictEqual('<div title="-">-</div>');
    }).not.toThrow();
  });

  it('RowFormatterDate WITH NULL DATE', () => {
    expect(() => {
      // Because you can never trust fscking M$ and their fscking schiit
      // @see: REPLENISHMENT-4369
      expect(shallow(<RowFormatterDate {...rowMock} value={null as any as string} />).html()).toStrictEqual(
        '<div title="-">-</div>'
      );
    }).not.toThrow();
  });

  it('RowFormatterDate WITH UNDEFINED DATE', () => {
    expect(() => {
      // Same as in the NULL test
      expect(shallow(<RowFormatterDate {...rowMock} value={undefined as any as string} />).html()).toStrictEqual(
        '<div title="-">-</div>'
      );
    }).not.toThrow();
  });

  it('RowFormatterDate WITH BROKEN DATE', () => {
    expect(() => {
      // Same as in the NULL test
      expect(shallow(<RowFormatterDate {...rowMock} value="BROKEN_STRING_DATE" />).html()).toStrictEqual(
        '<div title="-">-</div>'
      );
    }).not.toThrow();
  });

  it('RowFormatterCorrectionReason', () => {
    const Formatter = RowFormatterCorrectionReason;
    const expectCorrectionReason = correctionReasons[1];
    expect(shallow(<Formatter {...rowMock} value={expectCorrectionReason} />).html()).toBe(
      `<div title="${expectCorrectionReason.name}">${expectCorrectionReason.name}</div>`
    );
  });

  it('RowFormatterVolume', () => {
    const Formatter = RowFormatterVolume;
    expect(
      shallow(
        <Formatter
          {...rowMock}
          column={{
            name: 'Объем',
            key: 'volume',
            idx: 0,
            width: 300,
            left: 100,
          }}
          row={createInterWarehouseRecommendationRow({ height: 10, width: 20, length: 30, setQuantity: 5 })}
        />
      ).html()
    ).toBe('<div>6000</div>');
  });

  it('RowFormatterPrice', () => {
    const Formatter = RowFormatterPrice;
    expect(
      shallow(
        <Formatter
          {...rowMock}
          column={{
            name: 'Цена',
            key: 'price',
            idx: 0,
            width: 300,
            left: 100,
          }}
          value={1234567}
        />
      ).html()
    ).toBe('<div>1 234 567.00</div>');
  });

  it('RowFormatterSum', () => {
    const Formatter = RowFormatterSum;
    expect(
      shallow(
        <Formatter
          {...rowMock}
          column={{
            name: 'Сумма',
            key: 'price',
            idx: 0,
            width: 300,
            left: 100,
          }}
          row={createRecommendationRow({})}
        />
      ).html()
    ).toBe('<div>4 685.88</div>');
  });

  it('RowFormatterStock', () => {
    const Formatter = RowFormatterStock;
    expect(
      shallow(
        <Formatter
          {...rowMock}
          column={{
            name: 'Сток',
            key: 'stock',
            idx: 0,
            width: 300,
            left: 100,
          }}
          row={createRecommendationRow({ stock: 5, stockOverall: 6 })}
          value="hello"
        />
      ).html()
    ).toBe('<div>5/6</div>');
  });

  it('RowFormatterPurchasePromoPeriod', () => {
    const Formatter = RowFormatterPurchasePromoPeriod;
    expect(
      shallow(
        <Formatter
          {...rowMock}
          column={{
            name: 'Период промо закупки',
            key: 'purchasePromoEnd',
            idx: 0,
            width: 300,
            left: 100,
          }}
          row={createRecommendationRow({ purchasePromoStart: '2020-07-05', purchasePromoEnd: '2020-07-25' })}
        />
      ).html()
    ).toBe('<div>05.07-25.07</div>');
  });

  it('RowFormatterSalePromoPeriod', () => {
    const Formatter = RowFormatterSalePromoPeriod;
    expect(
      shallow(
        <Formatter
          {...rowMock}
          column={{
            name: 'Период промо продажи',
            key: 'salePromoEnd',
            idx: 0,
            width: 300,
            left: 100,
          }}
          row={createRecommendationRow({ salePromoStart: '2020-07-05', salePromoEnd: '2020-07-25' })}
        />
      ).html()
    ).toBe('<div>05.07-25.07</div>');
  });
});
