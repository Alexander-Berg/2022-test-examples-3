import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupTestProvider } from 'src/test/utils';
import { PromoInfo } from './PromoInfo';
import { OTraceResponse } from 'src/entities/otraceReport';

describe('<PromoInfo />', () => {
  it('renders links', () => {
    const { Provider } = setupTestProvider();
    const app = render(
      <Provider initialLocation={{ search: '?shopPromoId=single-plus-developer' }}>
        <PromoInfo />
      </Provider>
    );
    expect(app.getByText('single-plus-developer')).toBeInTheDocument();
    expect(app.container.getElementsByTagName('a')).toHaveLength(4);
  });

  it('fill select with promo ids', () => {
    const { Provider, doctorApi, api } = setupTestProvider();
    doctorApi.idxApiDataSource.loadSingleReport('123', 456);
    api.serviceProxyController.proxyGet.next().resolve({
      offer: {
        offer_id: '123',
        feed_id: 456,
        promos: [{ shopPromoId: 'Highlander' }, { shopPromoId: 'Duncan MacLeod' }],
      },
    } as unknown as OTraceResponse);

    const app = render(
      <Provider>
        <PromoInfo />
      </Provider>
    );
    expect(app.container.getElementsByTagName('a')).toHaveLength(0);

    userEvent.click(app.getByText('Выберите акцию из списка или введите свою'));
    userEvent.click(app.getAllByText('Duncan MacLeod')[0]);

    expect(app.container.getElementsByTagName('a')).toHaveLength(4);
  });
});
