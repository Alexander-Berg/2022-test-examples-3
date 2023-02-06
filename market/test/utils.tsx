import React, { FC } from 'react';
import { createBrowserHistory, LocationDescriptorObject } from 'history';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';
import { MemoryRouter } from 'react-router-dom';
import { createApiMock, createApiObjectMock } from '@yandex-market/mbo-test-utils';

import { createFetchApi, FetchAPI } from '../java/definitions';
import Api from '../api/Api';
import { DoctorApi } from 'src/store/DoctorApi';
import { DoctorApiContext } from 'src/context/DoctorApiContext';

interface ProviderProps {
  initialLocation?: Partial<LocationDescriptorObject>;
}

export function setupTestProvider() {
  const history = createBrowserHistory();

  history.replace('/');

  const fetchApi: FetchAPI = createFetchApi();
  const api = createApiObjectMock(createApiMock(new Api(fetchApi)));

  const doctorApi = new DoctorApi(api);

  const Provider: FC<ProviderProps> = ({ children, initialLocation }) => {
    history.location = {
      ...history.location,
      ...initialLocation,
    };
    return (
      <DoctorApiContext.Provider value={doctorApi}>
        <QueryParamsProvider history={history}>
          <MemoryRouter initialEntries={initialLocation ? [initialLocation] : undefined}>{children}</MemoryRouter>
        </QueryParamsProvider>
      </DoctorApiContext.Provider>
    );
  };

  return { Provider, api, doctorApi };
}
