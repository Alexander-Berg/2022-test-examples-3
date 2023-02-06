import React from 'react';
import { screen, act, fireEvent, render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { OfferFinderForm } from './OfferFinderForm';
import { setupTestProvider } from 'src/test/utils';
import { otraceResponse } from 'src/test/mockData/otraceResponse';
import Api from 'src/api/Api';
import { DoctorApi } from 'src/store/DoctorApi';

const selectIdType = (type: string) => {
  userEvent.click(screen.getByRole('listbox'));
  userEvent.click(screen.getByText(type));
};

const typeCompanyId = (id: string) => {
  const companyIdField = screen.getByRole('textbox', { name: 'ID компании' });
  userEvent.type(companyIdField, id);
};

const clearCompanyId = () => {
  const companyIdField = screen.getByRole('textbox', { name: 'ID компании' });
  userEvent.clear(companyIdField);
};

const typeShopSku = (id: string) => {
  const skuField = screen.getByRole('textbox', { name: 'Shop SKU*' });
  userEvent.type(skuField, id);
};

const submitForm = () => {
  fireEvent.submit(screen.getByTitle('Форма поиска офера'));
};

describe('<OfferFinderForm />', () => {
  let api: MockedApiObject<Api>;
  let doctorApi: DoctorApi;

  beforeEach(() => {
    const { Provider, api: dataApi, doctorApi: doctorApiTest } = setupTestProvider();
    api = dataApi;
    doctorApi = doctorApiTest;
    render(
      <Provider>
        <OfferFinderForm />
      </Provider>
    );
  });

  test('input business id', async () => {
    typeCompanyId('723377');
    typeShopSku('867');

    submitForm();

    expect(window.location.search).toBe('?businessId=723377&offerId=867');
  });

  test('search by shop id', async () => {
    selectIdType('Shop ID');
    typeCompanyId('1035100');
    typeShopSku('867');

    submitForm();

    // проверяем что уходит запрос получение businessId по shopId и резолвим его
    expect(api.idConverterController.convertShopToBusiness.activeRequests()).toHaveLength(1);
    act(() => {
      api.idConverterController.convertShopToBusiness.next().resolve(723377);
    });

    // в урл должен проставится shopId, businessId, offerId
    await waitFor(() => expect(window.location.search).toBe('?businessId=723377&offerId=867&shopId=1035100'));
  });

  test('input campaign id', async () => {
    selectIdType('Campaign ID');
    typeCompanyId('21995831');
    typeShopSku('867');

    submitForm();

    // проверяем что уходит запрос получение businessId по campaignId и резолвим его
    expect(api.idConverterController.convertCampaignToShopAndBusiness.activeRequests()).toHaveLength(1);
    act(() => {
      api.idConverterController.convertCampaignToShopAndBusiness.next().resolve({ businessId: 723377, shopId: 0 });
    });

    // в урл должен проставиться campaignId, businessId, offerId
    await waitFor(() => expect(window.location.search).toBe('?businessId=723377&campaignId=21995831&offerId=867'));
  });

  test('input waremd5', async () => {
    const waremd5 = 'y2iEEBaarUcMoINVf9KrmQ';
    const businessId = 723377;

    selectIdType('Waremd5');
    typeCompanyId(waremd5);

    submitForm();

    expect(api.serviceProxyController.activeRequests()).toHaveLength(1);
    act(() => {
      api.serviceProxyController.proxyGet.next().resolve(otraceResponse);
    });

    // проверяем что уходит запрос получение businessId по shopId и резолвим его
    await waitFor(() => expect(api.idConverterController.convertShopToBusiness.activeRequests()).toHaveLength(1));
    act(() => {
      api.idConverterController.convertShopToBusiness.next().resolve(businessId);
    });

    // в урл должен проставиться waremd5, businessId, offerId
    await waitFor(() =>
      expect(window.location.search).toBe(`?businessId=${businessId}&offerId=867&waremd5=${waremd5}`)
    );
  });

  test('input classifierMagicId', async () => {
    const classifierMagicId = 'testiktestovichtestikov';
    const businessId = 723377;

    selectIdType('Classifier Magic Id');
    typeCompanyId(classifierMagicId);

    submitForm();

    expect(api.serviceProxyController.activeRequests()).toHaveLength(1);
    act(() => {
      api.serviceProxyController.proxyGet.next().resolve({ offers: [{ shop_id: 123, shop_offer_id: 123 }] });
    });

    // проверяем что уходит запрос получение businessId по shopId и резолвим его
    await waitFor(() => expect(api.idConverterController.convertShopToBusiness.activeRequests()).toHaveLength(1));
    act(() => {
      api.idConverterController.convertShopToBusiness.next().resolve(businessId);
    });

    // в урл должны проставиться, businessId, offerId
    await waitFor(() => expect(window.location.search).toBe(`?businessId=${businessId}&offerId=123`));
  });

  it('display business id existence hint', () => {
    // отобразим сообщение о найденном business id
    doctorApi.existenceDataSource.requestData({ businessId: 123 });
    act(() => {
      api.inputValidationController.businessIdExists.next().resolve(true);
    });
    typeCompanyId('123');

    expect(screen.getByTitle('Business Id найден')).toBeTruthy();

    // отобразим сообщение о не найденном business id
    doctorApi.existenceDataSource.requestData({ businessId: 456 });

    act(() => {
      api.inputValidationController.businessIdExists.next().resolve(false);
    });

    clearCompanyId();
    typeCompanyId('456');
    expect(screen.getByTitle('Мы не смогли найти такой business Id')).toBeTruthy();

    // без дополнительных запросов достать уже вычисленное значение из хранилища
    clearCompanyId();
    typeCompanyId('123');
    expect(screen.getByTitle('Business Id найден')).toBeTruthy();

    // не выводить вообще ничего, если нет ни активного запроса ни информации
    clearCompanyId();
    typeCompanyId('111');
    expect(screen.queryAllByTitle('Business Id найден')).toHaveLength(0);
    expect(screen.queryAllByTitle('Мы не смогли найти такой business Id')).toHaveLength(0);
  });
});
