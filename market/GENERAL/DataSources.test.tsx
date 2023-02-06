import React from 'react';
import { act, render } from '@testing-library/react';

import Api from 'src/api/Api';
import { DoctorApi } from 'src/store/DoctorApi';
import { useDoctorApi } from 'src/hooks/useDoctorApi';
import { createFetchApi } from 'src/java/definitions';
import { DoctorApiContext } from 'src/context/DoctorApiContext';
import { DataSources } from './DataSources';

describe('<DataSources />', () => {
  it('apply middleware to request', async () => {
    const testResponseBody = { testik: 'testovich' };
    // @ts-ignore
    // eslint-disable-next-line no-global-assign
    fetch = jest.fn(() => {
      return Promise.resolve(new Response(JSON.stringify(testResponseBody)));
    });

    const api = new Api(createFetchApi());
    const doctorApi = new DoctorApi(api);

    await act(async () => {
      render(
        <DoctorApiContext.Provider value={doctorApi}>
          <DataFetcherComp>
            <DataSources />
          </DataFetcherComp>
        </DoctorApiContext.Provider>
      );
    });

    const dataSource = doctorApi.datacampDataSource.requestsInfo;
    const datacampRequestInfo = Object.values(dataSource)[0];
    expect(datacampRequestInfo?.response.body).toEqual(JSON.stringify(testResponseBody));
  });

  it('process failed request', async () => {
    const testResponseBody = { testik: 'testovich' };
    // @ts-ignore
    // eslint-disable-next-line no-global-assign
    fetch = jest.fn(() => {
      return Promise.reject(
        new Response(JSON.stringify(testResponseBody), {
          status: 500,
          headers: { head1: 'VL', head2: 'LL' },
        })
      );
    });

    const doctorApi = new DoctorApi(new Api(createFetchApi()));

    await act(async () => {
      render(
        <DoctorApiContext.Provider value={doctorApi}>
          <DataFetcherComp>
            <DataSources />
          </DataFetcherComp>
        </DoctorApiContext.Provider>
      );
    });

    const dataSource = doctorApi.datacampDataSource.requestsInfo;
    const datacampRequestInfo = Object.values(dataSource)[0];
    expect(datacampRequestInfo.response.body).toEqual(JSON.stringify(testResponseBody));
    expect(datacampRequestInfo.response.status).toEqual(500);
    expect(datacampRequestInfo.response.headers).toMatchObject({ head1: 'VL', head2: 'LL' });
  });
});

function DataFetcherComp(props: { children: JSX.Element }) {
  const doctorApi = useDoctorApi();
  doctorApi.datacampDataSource.requestData({ businessId: 0, shopSku: '0' });
  return props.children;
}
