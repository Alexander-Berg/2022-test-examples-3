import React from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { CategoryParameterType, FilterConfigDto, ParameterHeaderDto } from 'src/java/definitions';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { api } from 'src/test/singletons/apiSingleton';
import { NavigationNodeFilterConfig } from 'src/widgets';
import { getParameterHeaderOptionLabel } from './utils';

const TEST_FILTERS_CONFIG: FilterConfigDto = {
  filters: [1, 2],
  advancedFilters: [3],
  id: 123,
};

const TEST_FILTERS: ParameterHeaderDto[] = [
  {
    id: 1,
    name: 'Фильтр Фильтров',
    type: CategoryParameterType.ENUM,
    xslName: 'ненужное поле ХАХАХАХАХ',
    common: true,
  },
  {
    id: 2,
    name: 'Не фильтр',
    type: CategoryParameterType.ENUM,
    xslName: '',
    common: false,
  },
  {
    id: 3,
    name: 'Ну может и фильтр',
    type: CategoryParameterType.ENUM,
    xslName: 'qwert qwertievich',
    common: false,
  },
  {
    id: 4,
    name: 'Лишний фильтр',
    type: CategoryParameterType.ENUM,
    xslName: 'Viktor Yandex',
    common: false,
  },
];

const onChange = jest.fn(res => res);

describe('<NavigationNodeFilterConfig/>', () => {
  it('renders without errors', async () => {
    const Provider = setupTestProvider();

    render(
      <Provider>
        <NavigationNodeFilterConfig nodeId={123} />
      </Provider>
    );

    await act(async () => {
      api.navigationNodeFilterConfigController.getFilterConfig.next().resolve({ data: TEST_FILTERS_CONFIG });
    });

    await act(async () => {
      api.navigationNodeFilterConfigController.getParameterHeaders.next().resolve({
        data: {
          items: TEST_FILTERS,
          limit: TEST_FILTERS.length,
          offset: 0,
          total: TEST_FILTERS.length,
        },
      });
    });
    expect(screen.getByText(getParameterHeaderOptionLabel(TEST_FILTERS[0]))).not.toBeNull();
    expect(screen.getByText(getParameterHeaderOptionLabel(TEST_FILTERS[1]))).not.toBeNull();
    expect(screen.getByText(getParameterHeaderOptionLabel(TEST_FILTERS[2]))).not.toBeNull();
    expect(screen.queryByText(getParameterHeaderOptionLabel(TEST_FILTERS[3]))).toBeNull();
  });

  it.skip('does not send redundant requests', async () => {
    const Provider = setupTestProvider();

    const view = render(
      <Provider>
        <NavigationNodeFilterConfig nodeId={123} />
      </Provider>
    );

    await act(async () => {
      api.navigationNodeFilterConfigController.getFilterConfig.next().resolve({ data: TEST_FILTERS_CONFIG });
    });

    view.rerender(
      <Provider>
        <NavigationNodeFilterConfig nodeId={456} />
      </Provider>
    );

    await act(async () => {
      api.navigationNodeFilterConfigController.getFilterConfig.next().resolve({ data: TEST_FILTERS_CONFIG });
    });

    view.rerender(
      <Provider>
        <NavigationNodeFilterConfig nodeId={123} />
      </Provider>
    );

    await act(async () => {
      api.navigationNodeFilterConfigController.getFilterConfig.next().resolve({ data: TEST_FILTERS_CONFIG });
    });

    expect(api.navigationNodeFilterConfigController.getParameterHeaders.activeRequests()).toHaveLength(2);
    await act(async () => {
      api.navigationNodeFilterConfigController.getParameterHeaders.next().resolve(null as any);
    });
    await act(async () => {
      api.navigationNodeFilterConfigController.getParameterHeaders.next().resolve(null as any);
    });
    expect(api.navigationNodeFilterConfigController.activeRequests()).toHaveLength(0);
  });

  it('renders with empty init data', async () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <NavigationNodeFilterConfig />
      </Provider>
    );

    expect(api.navigationNodeFilterConfigController.getParameterHeaders.activeRequests()).toHaveLength(0);
  });

  it('submits correct values', async () => {
    const filterConfigId = 123;
    const Provider = setupTestProvider();
    render(
      <Provider>
        <NavigationNodeFilterConfig nodeId={999} onChange={onChange} />
      </Provider>
    );

    await act(async () => {
      api.navigationNodeFilterConfigController.getFilterConfig.next().resolve({
        data: {
          filters: [],
          advancedFilters: [],
          id: filterConfigId,
        },
      });
    });

    await act(async () => {
      api.navigationNodeFilterConfigController.getParameterHeaders.next().resolve({
        data: {
          items: TEST_FILTERS,
          limit: TEST_FILTERS.length,
          offset: 0,
          total: TEST_FILTERS.length,
        },
      });
    });

    // eslint-disable-next-line testing-library/no-node-access
    const filtersSelector = (await screen.findByText('Фильтры на выдаче')).parentElement?.getElementsByTagName(
      'input'
    )[0];

    // eslint-disable-next-line testing-library/no-node-access
    const advancedFiltersSelector = (await screen.findByText('Расширенные фильтры')).parentElement
      // eslint-disable-next-line testing-library/no-node-access
      ?.getElementsByTagName('input')[0];

    expect(filtersSelector).toBeTruthy();
    expect(advancedFiltersSelector).toBeTruthy();
    userEvent.click(filtersSelector!);

    userEvent.click(await screen.findByText(getParameterHeaderOptionLabel(TEST_FILTERS[1])));

    expect(onChange).toHaveLastReturnedWith({
      advancedFilters: [],
      filters: [TEST_FILTERS[1].id],
      id: filterConfigId,
    });

    userEvent.click(advancedFiltersSelector!);
    userEvent.click(await screen.findByText(getParameterHeaderOptionLabel(TEST_FILTERS[0])));

    expect(onChange).toHaveLastReturnedWith({
      advancedFilters: [TEST_FILTERS[0].id],
      filters: [TEST_FILTERS[1].id],
      id: filterConfigId,
    });

    userEvent.click(await screen.findByText('Скопировать в расширенные'));
    expect(onChange).toHaveLastReturnedWith({
      advancedFilters: [TEST_FILTERS[1].id],
      filters: [TEST_FILTERS[1].id],
      id: filterConfigId,
    });

    userEvent.click(await screen.findByText('Добавить все'));

    expect(onChange).toHaveLastReturnedWith({
      advancedFilters: [TEST_FILTERS[0].id, TEST_FILTERS[1].id, TEST_FILTERS[2].id, TEST_FILTERS[3].id],
      filters: [TEST_FILTERS[1].id],
      id: filterConfigId,
    });
  });

  it('sends correct filters in case initial filters data inconsistent', async () => {
    const filterConfigId = 0;
    const Provider = setupTestProvider();

    render(
      <Provider>
        <NavigationNodeFilterConfig nodeId={1} onChange={onChange} />
      </Provider>
    );

    await act(async () => {
      api.navigationNodeFilterConfigController.getFilterConfig.next().resolve({
        data: {
          filters: [5, 2],
          advancedFilters: [3],
          id: filterConfigId,
        },
      });
    });

    await act(async () => {
      api.navigationNodeFilterConfigController.getParameterHeaders.next().resolve({
        data: {
          items: TEST_FILTERS,
          limit: TEST_FILTERS.length,
          offset: 0,
          total: TEST_FILTERS.length,
        },
      });
    });

    expect(onChange).toHaveLastReturnedWith({
      advancedFilters: [TEST_FILTERS[2].id],
      filters: [TEST_FILTERS[1].id],
      id: filterConfigId,
    });
  });

  it('removes values correctly', async () => {
    const filterConfigId = 0;
    const Provider = setupTestProvider();

    render(
      <Provider>
        <NavigationNodeFilterConfig nodeId={1} onChange={onChange} />
      </Provider>
    );

    await act(async () => {
      api.navigationNodeFilterConfigController.getFilterConfig.next().resolve({
        data: {
          filters: [5, 2],
          advancedFilters: [3],
          id: filterConfigId,
        },
      });
    });

    await act(async () => {
      api.navigationNodeFilterConfigController.getParameterHeaders.next().resolve({
        data: {
          items: TEST_FILTERS,
          limit: TEST_FILTERS.length,
          offset: 0,
          total: TEST_FILTERS.length,
        },
      });
    });

    const clearButtons = screen.queryAllByText('Удалить все');

    userEvent.click(clearButtons[0]);

    expect(onChange).toHaveLastReturnedWith({
      advancedFilters: [TEST_FILTERS[2].id],
      filters: [],
      id: filterConfigId,
    });

    userEvent.click(clearButtons[1]);

    expect(onChange).toHaveLastReturnedWith({
      advancedFilters: [],
      filters: [],
      id: filterConfigId,
    });
  });
});
