import React from 'react';
import { StockQuantity } from './StockQuantity';
import { render } from '@testing-library/react';

const stockInfo = {
  market_stocks: {
    count: 225,
    meta: {
      source: 'MARKET_STOCK',
      timestamp: {
        nanos: 812847000,
        seconds: 1638427722,
      },
    },
  },
  partner_stocks: {
    count: 0,
    meta: {
      applier: 'QPARSER',
      source: 'PUSH_PARTNER_FEED',
      timestamp: {
        nanos: 191597000,
        seconds: 1638515558,
      },
    },
  },
};

describe('<StockQuantity />', () => {
  it('renders compact mode', () => {
    const app = render(<StockQuantity stockInfo={stockInfo} />);

    app.getByText('Кол-во на складе', { exact: false });
    app.getByText('225', { exact: false });
  });
});
