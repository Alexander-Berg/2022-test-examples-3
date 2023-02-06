import React from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ROUTES } from 'src/constants/routes';
import { FormalizationStat, PAGE_TITLE } from 'src/pages/Formalization/Stat/FormalizationStat';
import { renderWithProvider } from 'src/test/setupTestProvider';
import { api } from 'src/test/singletons/apiSingleton';
import { LogsEntityStat } from 'src/java/definitions';

const categories = [
  {
    guruCategoryId: 0,
    guruCategoryName: '',
    hid: 989024,
    name: 'Товары для мам и малышей/Здоровье и уход/Средства для купания',
    parentHid: -1,
    published: true,
    tovarId: 0,
    visual: true,
  },
];

const categoryStat = [
  {
    id: 989024,
    name: 'Товары для мам и малышей/Здоровье и уход/Средства для купания',
    formalised: 199220,
    totalOffers: 100,
    trashOffers: 5,
    published: true,
  },
];

const shopStat = [{ id: 20, name: 'my-shop', formalised: 52, totalOffers: 60, trashOffers: 7, published: true }];

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

const parameterValueStats = [
  {
    formalised: 42161,
    id: 12113983,
    name: '90',
    published: true,
    totalOffers: 957250,
    trashOffers: 0,
  },
];

const session = ['a', 'b'];

/**
 * проверяет отображения значений статистики
 */
const checkStat = async (stat: LogsEntityStat) => {
  await screen.findByText(stat.name);
  await screen.findByText(stat.formalised.toString());
  await screen.findByText(stat.totalOffers.toString());
};

const defaultResolves = async () => {
  await act(async () => {
    await api.formalizationFilterController.getSessions.next().resolve(session);
    await api.formalizationController.getVisualCategories.next().resolve(categoryStat);
    await api.categoryTreeController.getCategories.next().resolve(categories);
  });

  expect(api.allActiveRequests).toEqual({});
};

const resolveParameterStat = async (stat: LogsEntityStat[]) => {
  await act(async () => {
    await api.formalizationController.getParametersStat.next().resolve(stat);
  });

  expect(api.allActiveRequests).toEqual({});
};

/*
 Тест был засипан тут: https://st.yandex-team.ru/MBO-40118
 Будет раскипан тут: https://st.yandex-team.ru/MBO-40124
*/

describe.skip('FormalizationStat', () => {
  test('Simple render and resolve requests ', async () => {
    renderWithProvider(<FormalizationStat />, { route: ROUTES.STAT_FORMALIZATION.path });

    // substring - из-за того что первая буква отрисовывается отдельно
    await screen.findByText(PAGE_TITLE.substring(1));

    await defaultResolves();

    await checkStat(categoryStat[0]);
  });

  test('Render with filter in urls', async () => {
    renderWithProvider(<FormalizationStat />, {
      route: `${ROUTES.STAT_FORMALIZATION.path}?hasFilters=0&hid=${categories[0].hid}&page=1&perPage=1000&shopId=${shopStat[0].id}&sessionId=${session[0]}`,
    });

    await act(async () => {
      await api.categoryTreeController.getCategories.next().resolve(categories);
      await api.formalizationFilterController.getSessions.next().resolve(session);
      await api.formalizationFilterController.getShopName
        .next(shopId => shopId === shopStat[0].id)
        .resolve(shopStat[0].name);

      // чекаем что фильтры уходят на получение статистики
      await api.formalizationController.getVisualCategories
        .next(
          filter =>
            filter?.hid === categories[0].hid && filter.shopId === shopStat[0].id && filter.sessionId === session[0]
        )
        .resolve(categoryStat);
    });

    await checkStat(categoryStat[0]);
  });

  test('Expand category parameter, resolve request', async () => {
    renderWithProvider(<FormalizationStat />, { route: ROUTES.STAT_FORMALIZATION.path });

    await defaultResolves();

    // разворачиваем параметр
    userEvent.click(screen.getAllByText('параметры')[0]);

    await resolveParameterStat(parameterStats);

    await checkStat(parameterStats[0]);
  });

  test('Expand shop statistic and its parameters', async () => {
    renderWithProvider(<FormalizationStat />, { route: ROUTES.STAT_FORMALIZATION.path });

    await defaultResolves();

    // разворачиваем статистику по магазинам
    userEvent.click(screen.getByText('магазины'));

    await act(async () => {
      await api.formalizationController.getShopsStat.next().resolve(shopStat);
    });

    expect(api.allActiveRequests).toEqual({});

    await checkStat(shopStat[0]);

    // разворачиваем параметры у магазина
    userEvent.click(screen.getAllByText('параметры')[1]);

    await resolveParameterStat(parameterStats);

    await checkStat(parameterStats[0]);

    // разворачиваем значения параметров магазина
    userEvent.click(screen.getByText('значения'));

    await act(async () => {
      await api.formalizationController.getValuesStat.next().resolve(parameterValueStats);
    });

    expect(api.allActiveRequests).toEqual({});

    await checkStat(parameterValueStats[0]);
  });
});
