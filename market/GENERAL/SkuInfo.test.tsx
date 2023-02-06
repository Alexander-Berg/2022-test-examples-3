import React from 'react';
import { act, render } from '@testing-library/react';
import { Category, ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { setupTestProvider } from 'src/test/utils';
import { SkuInfo } from './SkuInfo';
import { IMboExporterModel } from 'src/entities/skuInfo/types';

describe('<SkuInfo />', () => {
  it('load and render data', async () => {
    const { Provider, api, doctorApi } = setupTestProvider();
    doctorApi.datacampDataSource.requestData({ businessId: 1, shopSku: '1' });

    // @ts-ignore
    doctorApi.datacampDataSource.setData(getDatacampResponse());

    const app = render(
      <Provider>
        <SkuInfo />
      </Provider>
    );

    await act(async () => {
      api.serviceProxyController.proxyGet.next((_, path) => path.includes('get_skus')).resolve(getSkusResponse());
      api.serviceProxyController.proxyGet
        .next((_, path) => path.includes('GetParameters'))
        .resolve(getParamsResponse());
    });

    expect(app.getByText('Boris')).toBeInTheDocument();
    expect(app.getByText('from snatch')).toBeInTheDocument();
    expect(app.getByText('0.1234')).toBeInTheDocument();
  });
});

function getParamsResponse() {
  return {
    category_parameters: {
      parameter: [
        {
          id: 987,
          service: false,
          value_type: ValueType.STRING,
          name: [{ lang_id: 225, name: 'TestikParamName' }],
        },
        {
          id: 654,
          service: false,
          value_type: ValueType.NUMERIC,
          name: [{ lang_id: 111, name: 'Darkwing Duck' }],
        },
        {
          id: 321,
          service: false,
          value_type: ValueType.BOOLEAN,
          name: [{ lang_id: 111, name: 'Duncan MacLeod' }],
        },
        {
          id: 111,
          service: false,
          value_type: ValueType.ENUM,
          name: [{ lang_id: 111, name: 'Boris' }],
          option: [{ id: 4, name: [{ name: 'from snatch' }] }],
        },
      ],
    } as Category,
  };
}

function getSkusResponse() {
  return {
    models: [
      {
        id: 23456,
        parameter_values: [
          {
            param_id: 987,
            xsl_name: 'testik',
            str_value: [{ value: 'ParamTestikValue' }],
          },
          {
            param_id: 654,
            xsl_name: 'Darkwing-Duck',
            numeric_value: '0.1234',
          },
          {
            param_id: 321,
            xsl_name: 'highlander',
            bool_value: false,
          },
          {
            param_id: 111,
            xsl_name: 'boris-the-blade',
            option_id: 4,
          },
        ],
        pictures: [{ url: 'http://pict_url', orig_md5: 'qwertyu' }],
      } as IMboExporterModel,
    ],
  };
}

function getDatacampResponse() {
  return {
    offer: {
      basic: {
        content: {
          binding: {
            uc_mapping: {
              market_category_id: 1,
              market_sku_id: 12,
            },
          },
        },
      },
    },
  };
}
