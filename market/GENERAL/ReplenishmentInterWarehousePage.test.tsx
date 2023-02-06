import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';
import MockDate from 'mockdate';

import { createInterWarehouseRecommendationRow } from 'src/test/data/interWarehouseRecommendations';
import { InterWarehouseActions, ReplenishmentInterWarehouseFilter } from './components';
import { ReplenishmentInterWarehousePage } from './ReplenishmentInterWarehousePage';
import { Attach, Button, DatePicker, Select, SelectOption } from 'src/components';
import { correctionReasons } from 'src/test/data/correctionReasons';
import { warehouses } from 'src/test/data/replenishmentWarehouses';
import { catteams } from 'src/test/data/replenishmentCatteams';
import { setupTestApp } from 'src/test/setupApp';
import Api from 'src/Api';
import {
  CatteamSelector,
  RangedDateFilter,
  WarehouseSelector,
} from './components/ReplenishmentInterWarehouseFilter/components';

const TODAY = '2019-12-31';
const MOCK_DATE_1 = '2020-01-01';
const MOCK_DATE_2 = '2020-01-02';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const withTargetValue: <T>(value: T) => { target: { value: T } } = value => ({ target: { value } });

beforeAll(() => {
  MockDate.set(TODAY);
});

beforeEach(() => {
  ({ app, api } = setupTestApp(`/replenishment/inter-warehouse`));
  api.replenishmentWarehouseController.getWarehouses.next().resolve(warehouses);
  api.interWarehouseRecommendationsController.getCatteams.next().resolve(catteams);
  api.correctionReasonController.getCorrectionReasons.next().resolve(correctionReasons);
  api.interWarehouseRecommendationsController.recommendations
    .next()
    .resolve([
      createInterWarehouseRecommendationRow({ msku: 1, setQuantity: 1, correctionReason: correctionReasons[0] }),
      createInterWarehouseRecommendationRow({ msku: 2, setQuantity: 1, correctionReason: correctionReasons[0] }),
      createInterWarehouseRecommendationRow({ msku: 3, setQuantity: 1, correctionReason: correctionReasons[0] }),
    ]);
  app.update();
});

afterAll(() => {
  MockDate.reset();
});

describe('<ReplenishmentInterWarehousePage />', () => {
  const getReplenishmentPage = () => app.find(ReplenishmentInterWarehousePage);

  it('Should mount correctly', () => {
    const page = getReplenishmentPage();

    expect(page).toHaveLength(1);
    expect(page.find(ReplenishmentInterWarehouseFilter)).toHaveLength(1);

    expect(api.interWarehouseRecommendationsController.recommendations).toBeCalled();
    expect(api.interWarehouseRecommendationsController.getCatteams).toBeCalled();
    expect(api.replenishmentWarehouseController.getWarehouses).toBeCalled();
  });

  it('Should set order date filters correctly', () => {
    // Initial filter state checks
    expect(api.interWarehouseRecommendationsController.recommendations).toBeCalledTimes(1);
    expect(api.interWarehouseRecommendationsController.recommendations).toHaveBeenCalledWith(
      {
        dateFrom: TODAY,
        dateTo: TODAY,
      },
      1,
      100
    );

    // Filter rendering test
    const filterBlock = app.find(ReplenishmentInterWarehouseFilter);
    expect(filterBlock).toHaveLength(1);

    const dateFilters = filterBlock.first().find(RangedDateFilter);
    expect(dateFilters).toHaveLength(1);

    // Order date range
    const orderDateFilters = dateFilters.first().find(DatePicker);
    expect(orderDateFilters).toHaveLength(2);

    // set dateFrom
    orderDateFilters.at(0).find('input').simulate('change', withTargetValue(MOCK_DATE_1));
    app.update();

    expect(api.interWarehouseRecommendationsController.recommendations).toBeCalledTimes(2);
    expect(api.interWarehouseRecommendationsController.recommendations).toHaveBeenLastCalledWith(
      {
        dateFrom: MOCK_DATE_1,
        dateTo: TODAY,
      },
      1,
      100
    );

    // set dateTo
    orderDateFilters.at(1).find('input').simulate('change', withTargetValue(MOCK_DATE_2));
    app.update();

    expect(api.interWarehouseRecommendationsController.recommendations).toBeCalledTimes(3);
    expect(api.interWarehouseRecommendationsController.recommendations).toHaveBeenLastCalledWith(
      {
        dateFrom: MOCK_DATE_1,
        dateTo: MOCK_DATE_2,
      },
      1,
      100
    );
  });

  it('Should set warehouse filter correctly', () => {
    // Initial filter state checks
    expect(api.interWarehouseRecommendationsController.recommendations).toBeCalledTimes(1);
    expect(api.interWarehouseRecommendationsController.recommendations).toHaveBeenCalledWith(
      {
        dateFrom: TODAY,
        dateTo: TODAY,
      },
      1,
      100
    );

    // Filter rendering test
    const filterBlock = app.find(ReplenishmentInterWarehouseFilter);
    expect(filterBlock).toHaveLength(1);

    // Warehouse selectors
    const warehouseSelector = filterBlock.find(WarehouseSelector);
    expect(warehouseSelector).toHaveLength(2);

    // set warehouseTo
    const warehouseToSelector = warehouseSelector.at(1).find(Select);
    expect(warehouseToSelector).toHaveLength(1);

    const warehouseToOptions: SelectOption[] = warehouseToSelector.first().prop('options');
    expect(warehouseToOptions).toHaveLength(warehouses.length + 1);

    warehouseToOptions.slice(1).forEach(({ label, value }, index) => {
      expect(label).toEqual(warehouses[index].name);
      expect(value).toEqual(`${warehouses[index].id}`);
    });

    act(() => {
      warehouseToSelector.first().props().onChange(warehouseToOptions[2]);
      app.update();
    });

    expect(api.interWarehouseRecommendationsController.recommendations).toBeCalledTimes(2);
    expect(api.interWarehouseRecommendationsController.recommendations).toHaveBeenLastCalledWith(
      {
        dateFrom: TODAY,
        dateTo: TODAY,
        warehouseTo: warehouses[1].id,
      },
      1,
      100
    );
  });

  it('Should set department filter correctly', async () => {
    // Initial filter state checks
    expect(api.interWarehouseRecommendationsController.recommendations).toBeCalledTimes(1);
    expect(api.interWarehouseRecommendationsController.recommendations).toHaveBeenCalledWith(
      {
        dateFrom: TODAY,
        dateTo: TODAY,
      },
      1,
      100
    );

    // Filter rendering test
    const filterBlock = app.find(ReplenishmentInterWarehouseFilter);
    expect(filterBlock).toHaveLength(1);

    // set catteam
    const catteamSelector = filterBlock.find(CatteamSelector);
    expect(catteamSelector).toHaveLength(1);

    const catteamSelectorComponent = catteamSelector.first().find(Select);
    expect(catteamSelectorComponent).toHaveLength(1);

    const catteamOptions: SelectOption[] = catteamSelectorComponent.first().prop('options');
    expect(catteamOptions).toHaveLength(catteams.length + 1);

    catteamOptions.slice(1).forEach(({ label, value }, index) => {
      const catteam = catteams[index];
      expect(catteam).toBeTruthy();

      expect(label).toEqual(catteam);
      expect(value).toEqual(catteam);
    });

    act(() => {
      catteamSelectorComponent.first().props().onChange(catteamOptions[2]);
      app.update();
    });

    expect(api.interWarehouseRecommendationsController.recommendations).toBeCalledTimes(2);
    expect(api.interWarehouseRecommendationsController.recommendations).toHaveBeenLastCalledWith(
      {
        catteam: catteams[1],
        dateFrom: TODAY,
        dateTo: TODAY,
      },
      1,
      100
    );
  });

  it('Should upload excel', () => {
    const file = new File(['(⌐□_□)'], 'myFile.xlsx', { type: 'application/vnd.ms-excel' });
    getReplenishmentPage()
      .find(Attach)
      .find('input')
      .simulate('change', { target: { files: [file], value: '' } });

    expect(api.interWarehouseRecommendationsController.uploadExcel).toBeCalledTimes(1);
    expect(api.interWarehouseRecommendationsController.uploadExcel).toBeCalledWith(file);
  });

  it('Validation button should work', () => {
    const actions = getReplenishmentPage().find(InterWarehouseActions);
    expect(actions).toHaveLength(1);

    const buttons = actions.find(Button);
    expect(buttons).toHaveLength(4);

    const validationButton = buttons.at(1);

    expect(validationButton.text()).toEqual('Проверить рекомендации');
    validationButton.simulate('click');

    expect(api.interWarehouseRecommendationsController.validateInterWarehouseMovementAvailability).toBeCalledTimes(1);
  });
});
