import React from 'react';
import userEvent from '@testing-library/user-event';
import { act, screen } from '@testing-library/react';

import { FormalizationView, VIEW_PAGE_TITLE } from 'src/pages/Formalization/View/FormalizationView';
import { FormalizationViewFilter, OfferData } from 'src/java/definitions';
import { renderWithProvider } from 'src/test/setupTestProvider';
import { api } from 'src/test/singletons/apiSingleton';
import { ROUTES } from 'src/constants/routes';

const categories = [
  {
    id: 989024,
    name: 'Товары для мам и малышей/Здоровье и уход/Средства для купания',
    formalised: 199220,
    totalOffers: 100,
    trashOffers: 5,
    published: true,
  },
];

const parameterStats = [
  {
    formalised: 11473380,
    id: 11903127,
    name: 'for_title',
    published: true,
    totalOffers: 11680856,
    trashOffers: 0,
  },
];

const shopStat = [{ id: 20, name: 'my-shop', formalised: 52, totalOffers: 60, trashOffers: 7, published: true }];

const offer = {
  categoryName: categories[0].name,
  description:
    'Специальный адаптер предназначен для заправки  масла в АКПП автомобилей Мерседес. Модификация автоматической трансмиссии, под которую разработан адаптер это 722.9. Адаптер оснащен быстроразъемным соединением с клапаном, что позволяет контролировать уровень масла в АКПП. Данный адаптер можно использовать с любой бочкой для заправки трансмиссионной жидкости. В набор также входит специальная оправка для сливной трубки, которая поставляется под оригинальным номером 722 589 03 15 00.|Код производителя: CT-1243',
  fromRobot: false,
  hid: 90508,
  highlighPosDescr: [],
  highlighPosOffer: [],
  highlighPosYml: [],
  hypothesisCategoryId: 0,
  hypothesisCategoryName: '',
  hypothesisValue: '',
  offer: 'Адаптер для заправки масла в АКПП Mercedes  722.9 Car-Tool CT-1243',
  offerId: 'fff432e9c7ee3cbf984353eae6e4152e',
  paramRulesApplied: {},
  paramValueIds: { 7893318: [9349919] },
  paramValues: {},
  shopId: 239800,
  shopName: 'autoscaners.ru',
  shopUrl: 'http://autoscaners.ru',
  url:
    'https://www.autoscaners.ru/catalogue/?catalogue_id=adapter_dlya_zapravki_masla_v_akpp_mercedes_722_9&utm_source=yandex_market&rs=yamarket8_21189196_5096',
  valueId: 0,
  ymlParams: ['vendor Car-tool '],
  ymlParamsAsXml:
    '<?xml version="1.0" encoding="UTF-8"?>\n<offer_params><param name="vendor" unit="">Car-tool</param></offer_params>',
};

const offerWithFormalization = {
  ...offer,
  highlighPosYml: [[58, 59, 60, 61, 62, 63, 337, 396, 397, 398, 399, 400, 401, 402, 403, 696]],
  // eslint-disable-next-line prettier/prettier
  highlighPosOffer: [],
  // eslint-disable-next-line prettier/prettier
  highlighPosDescr: [],
  paramValues: {
    7893318: ['Car-tool'],
  },
  ymlParams: [
    '<font color=red>*</font><span class="f_param popup_title">vendor<em>Выбранный параметр Производитель, ~250.0<br><span class=f_param>Паттерны:</span><br><span><a style="text-decoration: none;" class="dynamic-link" target="_blank" href="/gwt/#tovarTree/hyperId=90508/parameters">6170818</a>&nbsp;Параметр-значение близко</span></em></span> <font color=red>*</font><span class="f_value popup_title">Car-tool<em>Выбранное значение Car-tool параметра Производитель, ~250.0<br><span class=f_param>Паттерны:</span><br><span><a style="text-decoration: none;" class="dynamic-link" target="_blank" href="/gwt/#tovarTree/hyperId=90508/parameters">6170818</a>&nbsp;Параметр-значение близко</span></em></span> ',
  ],
};

const sessions = ['20201124_1225'];

const defaultResolves = () => {
  act(() => {
    api.formalizationFilterController.getSessions.next().resolve(sessions);
    api.formalizationFilterController.getOperators.next().resolve([]);
    api.formalizationController.getVisualCategories.next().resolve(categories);
  });
};

const resolveLoadOffers = async (offers: OfferData[]) => {
  await act(async () => {
    await api.formalizationViewController.getOffers.next().resolve(offers);
    await api.formalizationViewController.getOffersCount.next().resolve(1);
  });
};

const checkFormalizationInfo = async () => {
  await screen.findByText(new RegExp('Выбранный параметр Производитель'));
  await screen.findByText(new RegExp('Выбранное значение Car-tool параметра'));
};

const PATH_WITH_OFFER = `${ROUTES.VIEW_FORMALIZATION.path}?hid=${categories[0].id}&offerId=${offer.offerId}`;

/*
 Тест был засипан тут: https://st.yandex-team.ru/MBO-40118
 Будет раскипан тут: https://st.yandex-team.ru/MBO-40120
*/

describe.skip('FormalizationView', () => {
  test('Simple render and resolve requests', async () => {
    renderWithProvider(<FormalizationView />, { route: ROUTES.VIEW_FORMALIZATION.path });

    defaultResolves();
    expect(api.allActiveRequests).toEqual({});

    // substring - из-за того что первая буква отрисовывается отдельно
    await screen.findByText(VIEW_PAGE_TITLE.substring(1));
  });

  test('Render with filters in urls', async () => {
    renderWithProvider(<FormalizationView />, {
      route: `${ROUTES.VIEW_FORMALIZATION.path}?hid=${categories[0].id}&page=0&sessionId=${sessions[0]}&shopId=${shopStat[0].id}`,
    });

    defaultResolves();

    act(() => {
      api.formalizationController.getParametersStat.next().resolve(parameterStats);
      api.formalizationFilterController.getShopName.next().resolve(shopStat[0].name);
    });

    // чекаем что запросы на получение оферов уходят с задаными фильтрами
    const filterRequestByFilter = (filter?: FormalizationViewFilter) =>
      filter?.hid === categories[0].id && filter.sessionId === sessions[0] && filter.shopId === shopStat[0].id;

    act(() => {
      api.formalizationViewController.getOffers.next(filterRequestByFilter).resolve([offer]);
      api.formalizationViewController.getOffersCount.next(filterRequestByFilter).resolve(1);
    });

    expect(api.allActiveRequests).toEqual({});

    await screen.findByText(shopStat[0].name);
  });

  test('Show and formalize offer', async () => {
    renderWithProvider(<FormalizationView />, {
      route: `${ROUTES.VIEW_FORMALIZATION.path}?hid=${categories[0].id}&page=0&sessionId=${sessions[0]}&shopId=${shopStat[0].id}`,
    });

    act(() => {
      api.formalizationController.getParametersStat.next().resolve(parameterStats);
      api.formalizationFilterController.getShopName.next().resolve(shopStat[0].name);
    });

    await resolveLoadOffers([offer]);
    defaultResolves();

    await screen.findByText(new RegExp(offer.description.substring(0, 30)));

    // формализуем один товар по кнопке из ячейки
    userEvent.click(screen.getByTestId('formalize-one-btn'));

    act(() => {
      api.formalizationViewController.callFormaliser.next().resolve(offerWithFormalization);
    });

    expect(api.allActiveRequests).toEqual({});

    // после формализации должны появиться тултипы с инфой формализации
    await checkFormalizationInfo();
  });

  test('Formalize all offers from column button', async () => {
    renderWithProvider(<FormalizationView />, {
      route: PATH_WITH_OFFER,
    });

    act(() => {
      api.formalizationController.getParametersStat.next().resolve(parameterStats);
    });

    await resolveLoadOffers([offer]);
    defaultResolves();

    // формализуем все товары по кнопке из заголовка таблицы
    userEvent.click(screen.getByTestId('formalize-all-btn'));
    api.formalizationViewController.callFormaliserBatch.next().resolve([offerWithFormalization]);
    expect(api.allActiveRequests).toEqual({});

    // после формализации должны появиться тултипы с инфой формализации
    await checkFormalizationInfo();
  });

  test('Formalize all offers from float panel', async () => {
    renderWithProvider(<FormalizationView />, {
      route: PATH_WITH_OFFER,
    });

    act(() => {
      api.formalizationController.getParametersStat.next().resolve(parameterStats);
    });

    await resolveLoadOffers([offer]);
    defaultResolves();

    // формализуем все товары по кнопке из плавающей менюшки
    userEvent.click(screen.getByText('Формализовать все'));
    api.formalizationViewController.callFormaliserBatch.next().resolve([offerWithFormalization]);
    expect(api.allActiveRequests).toEqual({});

    // после формализации должны появиться тултипы с инфой формализации
    await checkFormalizationInfo();
  });

  test('Refresh formalization', async () => {
    renderWithProvider(<FormalizationView />, {
      route: PATH_WITH_OFFER,
    });

    await act(async () => {
      await api.formalizationController.getParametersStat.next().resolve(parameterStats);
    });

    await resolveLoadOffers([offer]);
    defaultResolves();

    const refreshBtn = await screen.findByText('Перезалить формализатор');
    userEvent.click(refreshBtn);

    act(() => {
      api.formalizationViewController.formalisationRefresh.next().resolve('');
    });

    expect(api.allActiveRequests).toEqual({});

    await screen.findByText(parameterStats[0].name);
  });

  test('Expand operator task creator', async () => {
    renderWithProvider(<FormalizationView />, { route: ROUTES.VIEW_FORMALIZATION.path });

    defaultResolves();

    userEvent.click(screen.getByText('Создать задание для оператора'));

    await screen.findByText('Ответственный:');
  });
});
