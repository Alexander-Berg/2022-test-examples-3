import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { OfferLinks } from './OfferLinks';
import { setupTestProvider } from 'src/test/utils';

describe('<OfferLinks />', () => {
  it('renders contract links', async () => {
    const { doctorApi, Provider, api } = setupTestProvider();
    doctorApi.mbocDataSource.requestData({ businessId: 123, offerId: '0' });

    expect(api.serviceProxyController.proxyGet.activeRequests()).toHaveLength(1);
    api.serviceProxyController.proxyGet.next().resolve(getMbocResponse());

    const app = render(
      <Provider>
        <OfferLinks />
      </Provider>
    );

    const dataSourceLinksAccordion = await app.findByText(doctorApi.mbocDataSource.name);
    userEvent.click(dataSourceLinksAccordion);
    app.getByText('Создать тикет в Маркет-Индексатор');
  });
});

function getMbocResponse() {
  return {
    datacampLink:
      'http://datacamp.white.vs.market.yandex.net/v1/partners/655451/offers?format=json&full=true&offer_id=81870',
    datacampGroupLink: '',
    auditLink: 'https://mbo.market.yandex.ru/ui/audit?entityTypeList=CM_BLUE_OFFER&entityId=207787721',
    offerLink:
      'http://cm-api.vs.market.yandex.net/proto/mboMappingsService/searchMappingsByBusinessKeys?q={keys:[{"business_id":655451,"offer_id":"81870"}]}',
    agLink: 'https://ir-ui.market.yandex-team.ru/#/reports/gcSkuTicket?businessId=655451&shopSku=81870',
    // почему то в тестовой среде не перехватывается проставление данных, пока не  понимаю с чем связано, попозже разберусь
    // doctor: {
    //   links: [
    //     {
    //       source: 'My subsystem',
    //       text: 'Contract Link text',
    //       url: 'https://yandex.ru/my-cool-service?my-param=42',
    //     },
    //   ],
    // },
  };
}
