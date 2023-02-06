import React from 'react';
import { render } from '@testing-library/react';

import { IndexerInfo } from './IndexerInfo';
import { setupTestProvider } from 'src/test/utils';
import { UnitedOffer } from 'src/entities/datacampOffer';

describe('<IndexerInfo />', () => {
  it('renders without errors', () => {
    const { Provider, doctorApi, api } = setupTestProvider();

    doctorApi.idxApiDataSource.loadIndexerCurrentMaster();
    api.serviceProxyController.proxyGet.next().resolve(JSON.stringify({ current_master: 'Morpheus' }));

    // @ts-ignore
    doctorApi.datacampDataSource.setData({
      offer: {
        basic: {
          identifiers: {
            offer_id: '456',
            business_id: 123,
          },
        },
      } as UnitedOffer,
    });

    const app = render(
      <Provider>
        <IndexerInfo />
      </Provider>
    );

    const link = app.getByText('Причина почему оффер не попал в выгрузку');
    expect(link).toBeInTheDocument();
    expect(link.getAttribute('href')?.includes(`offer_id%20==%20'${456}'`)).toBeTruthy();
  });
});
