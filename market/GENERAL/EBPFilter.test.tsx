import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';

import { StockCoverEditor } from '../../../BulkEdit/components';
import { DemandType, FilterType, WeeksType } from 'src/java/definitions-replenishment';
import { recommendationsCount } from 'src/test/data/recomendationsCount';
import { createRecommendation } from 'src/test/data/recomendations';
import { correctionReasons } from 'src/test/data/correctionReasons';
import { ReplenishmentFilter } from '../../ReplenishmentFilter';
import { FilterSelector, WeekSelector } from './components';
import { createDemandDTO } from 'src/test/data/demand';
import { setupTestApp } from 'src/test/setupApp';
import { BulkEdit } from '../../../BulkEdit';
import { Select } from 'src/components';
import Api from 'src/Api';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const DEMAND_ID_1 = 1111;
const DEMAND_ID_2 = 2222;
const WEEKS = undefined;
const USER_FILTER = undefined;

const RECOMMENDATIONS_MOCK = [
  createRecommendation({
    msku: 1,
    setQuantity: 1,
    vendorId: 111,
    manufacturer: '101',
    correctionReason: { name: '', id: 0 },
    length: 300,
    height: 100,
    width: 100,
    weight: 1000,
  }),
  createRecommendation({
    msku: 2,
    setQuantity: 1,
    vendorId: 111,
    manufacturer: '102',
    correctionReason: { name: '', id: 0 },
    length: 100,
    height: 200,
    width: 100,
    weight: 3000,
  }),
  createRecommendation({
    msku: 3,
    setQuantity: 1,
    vendorId: 222,
    manufacturer: '102',
    correctionReason: { name: '', id: 0 },
    length: 100,
    height: 100,
    width: 100,
    weight: 2000,
  }),
];

beforeEach(() => {
  ({ app, api } = setupTestApp(`/replenishment/ebp/?demandIds=${DEMAND_ID_1}&demandIds=${DEMAND_ID_2}`));

  api.demandController.demands
    .next()
    .resolve([createDemandDTO({ id: DEMAND_ID_1 }), createDemandDTO({ id: DEMAND_ID_2 })]);

  api.recommendationController.replenishmentWithCountPost.next().resolve({
    recommendations: RECOMMENDATIONS_MOCK,
    numberOfCorrectedRecommendations: 0,
    userFiltersCount: [],
    count: recommendationsCount,
  });

  api.correctionReasonController.getCorrectionReasons.next().resolve(correctionReasons);
  app.update();
});

describe('<EBPFilter />', () => {
  // Filter section
  const getReplenishmentFilter = () => app.find(ReplenishmentFilter);

  const getFilterSelector = () => getReplenishmentFilter().find(FilterSelector).find(Select);
  const getFilterOptions = () => getFilterSelector().props().options;

  const getWeekSelector = () => getReplenishmentFilter().find(WeekSelector).find(Select);
  const getWeeksOptions = () => getWeekSelector().props().options;

  // Bulk edit section
  const getBulkEdit = () => app.find(BulkEdit);
  const getStockCoverEditorComponent = () => getBulkEdit().find(StockCoverEditor);

  it('Should render correctly', () => {
    expect(getReplenishmentFilter()).toHaveLength(1);
    expect(getFilterSelector()).toHaveLength(1);
  });

  it('Should correctly receive filter list', () => {
    const filterSelector = getFilterSelector();
    expect(filterSelector).toHaveLength(1);

    const filterOptions = getFilterOptions();
    expect(filterOptions).toHaveLength(10);

    expect(filterOptions[0].value.id).toEqual(FilterType.ALL);
    expect(filterOptions[1].value.id).toEqual(FilterType.PROCESSED);
    expect(filterOptions[2].value.id).toEqual(FilterType.NEED_MANUAL_REVIEW);
    expect(filterOptions[3].value.id).toEqual(FilterType.NEW);
    expect(filterOptions[4].value.id).toEqual(FilterType.SALES_ZERO);
    expect(filterOptions[5].value.id).toEqual(FilterType.SALES_ZERO_AND_POSITIVE_STOCK_OR_TRANSIT);
    expect(filterOptions[6].value.id).toEqual(FilterType.SC);
    expect(filterOptions[7].value.id).toEqual(FilterType.SPECIAL_ORDER);
    expect(filterOptions[8].value.id).toEqual(FilterType.TRANSIT_WARNING);
    expect(filterOptions[9].value.id).toEqual(FilterType.ASSORTMENT_GOODS_SUB_SSKU);
  });

  it('Should render correctly', () => {
    expect(getReplenishmentFilter()).toHaveLength(1);
    expect(getBulkEdit()).toHaveLength(1);

    // Default state
    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: [DEMAND_ID_1, DEMAND_ID_2],
          weeks: WEEKS,
        },
        userFilter: USER_FILTER,
      },
      DemandType.TYPE_1P
    );
  });

  it('Should select filter', async () => {
    const select = getFilterSelector();
    expect(select).toHaveLength(1);

    const filterOptions = getFilterOptions();
    expect(filterOptions).toHaveLength(10);

    expect(filterOptions[0].value.id).toEqual(FilterType.ALL);
    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: [DEMAND_ID_1, DEMAND_ID_2],

          weeks: WEEKS,
        },
        userFilter: USER_FILTER,
      },
      DemandType.TYPE_1P
    );

    expect(filterOptions[1].value.id).toEqual(FilterType.PROCESSED);
    await act(async () => {
      await select.props().onChange(filterOptions[1]);
      app.update();
    });

    expect(getStockCoverEditorComponent()).toHaveLength(1);
    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: [DEMAND_ID_1, DEMAND_ID_2],
          filter: FilterType.PROCESSED,
          weeks: WEEKS,
        },
        userFilter: USER_FILTER,
      },
      DemandType.TYPE_1P
    );

    expect(filterOptions[3].value.id).toEqual(FilterType.NEW);
    await act(async () => {
      await select.props().onChange(filterOptions[3]);
      app.update();
    });

    expect(getStockCoverEditorComponent()).toHaveLength(1);
    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: [DEMAND_ID_1, DEMAND_ID_2],
          filter: FilterType.NEW,
          weeks: WEEKS,
        },
        userFilter: USER_FILTER,
      },
      DemandType.TYPE_1P
    );
  });

  it('Should select weeks', async () => {
    const filterOption = getFilterOptions()[4];
    expect(filterOption.value.id).toEqual(FilterType.SALES_ZERO);
    getFilterSelector().props().onChange(filterOption);
    app.update();

    const options = getWeeksOptions();
    const select = getWeekSelector();
    expect(select).toHaveLength(1);

    expect(options[0].value.id).toEqual(WeeksType.ZERO_TWO);
    await act(async () => {
      await select.props().onChange(options[0]);
      app.update();
    });

    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: [DEMAND_ID_1, DEMAND_ID_2],
          filter: FilterType.SALES_ZERO,
          weeks: WeeksType.ZERO_TWO,
        },
        userFilter: USER_FILTER,
      },
      DemandType.TYPE_1P
    );

    expect(options[1].value.id).toEqual(WeeksType.TWO_THREE);
    await act(async () => {
      await select.props().onChange(options[1]);
      app.update();
    });

    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: [DEMAND_ID_1, DEMAND_ID_2],
          filter: FilterType.SALES_ZERO,
          weeks: WeeksType.TWO_THREE,
        },
        userFilter: USER_FILTER,
      },
      DemandType.TYPE_1P
    );

    expect(options[2].value.id).toEqual(WeeksType.THREE_FOUR);
    await act(async () => {
      await select.props().onChange(options[2]);
      app.update();
    });

    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: [DEMAND_ID_1, DEMAND_ID_2],
          filter: FilterType.SALES_ZERO,
          weeks: WeeksType.THREE_FOUR,
        },
        userFilter: USER_FILTER,
      },
      DemandType.TYPE_1P
    );

    expect(options[3].value.id).toEqual(WeeksType.FOUR_EIGHT);
    await act(async () => {
      await select.props().onChange(options[3]);
      app.update();
    });

    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: [DEMAND_ID_1, DEMAND_ID_2],
          filter: FilterType.SALES_ZERO,
          weeks: WeeksType.FOUR_EIGHT,
        },
        userFilter: USER_FILTER,
      },
      DemandType.TYPE_1P
    );
  });
});
