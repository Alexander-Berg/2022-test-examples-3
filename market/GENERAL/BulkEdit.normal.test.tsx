import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';

import {
  DemandType,
  FilterType,
  RecommendationUserFilter,
  WeeksType,
  WarningFilterType,
  StockCoverType,
} from 'src/java/definitions-replenishment';
import { QuantumEditor, SelectionEditor, StockCoverEditor, TransitEditor } from './components';
import { FilterSelector } from '../ReplenishmentFilter/components/EBPFilter/components';
import { recommendationsCount } from 'src/test/data/recomendationsCount';
import { createRecommendation } from 'src/test/data/recomendations';
import { correctionReasons } from 'src/test/data/correctionReasons';
import { ReplenishmentFilter } from '../ReplenishmentFilter';
import { Button, Select, TextInput } from 'src/components';
import { createDemandDTO } from 'src/test/data/demand';
import { setupTestApp } from 'src/test/setupApp';
import { BulkEdit } from './BulkEdit';
import Api from 'src/Api';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const DEMAND_ID_1 = 1111;
const DEMAND_ID_2 = 2222;
const USER_FILTER: RecommendationUserFilter[] = [];
const FILTER = FilterType.NEW;
const WEEKS = undefined;

const DEMAND_IDS = [DEMAND_ID_1, DEMAND_ID_2];
const DEMAND_KEYS = DEMAND_IDS.map(id => ({ id, version: 1 }));

const RECOMMENDATIONS_MOCK = [
  createRecommendation({
    msku: 1,
    setQuantity: 1,
    vendorId: 111,
    manufacturer: '101',
    demandId: DEMAND_ID_1,
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
    demandId: DEMAND_ID_2,
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
    demandId: DEMAND_ID_1,
    correctionReason: { name: '', id: 0 },
    length: 100,
    height: 100,
    width: 100,
    weight: 2000,
  }),
];

beforeEach(() => {
  ({ app, api } = setupTestApp(
    `/replenishment/ebp/?demandIds=${DEMAND_ID_1}&demandIds=${DEMAND_ID_2}&filter=${FILTER}`
  ));

  api.demandController.demands
    .next()
    .resolve([createDemandDTO({ id: DEMAND_ID_1 }), createDemandDTO({ id: DEMAND_ID_2 })]);

  api.recommendationController.replenishmentWithCountPost.next().resolve({
    recommendations: RECOMMENDATIONS_MOCK,
    numberOfCorrectedRecommendations: 0,
    userFiltersCount: [],
    count: recommendationsCount,
  });

  api.demandController.getWarningCounts.next().resolve({} as Record<WarningFilterType, number>);
  api.correctionReasonController.getCorrectionReasons.next().resolve(correctionReasons);
  app.update();
});

describe('<BulkEdit />', () => {
  // Filter section
  const getReplenishmentFilter = () => app.find(ReplenishmentFilter);
  const getFilterSelector = () => getReplenishmentFilter().find(FilterSelector).find(Select);
  const getFilterOptions = () => getFilterSelector().props().options;

  // Bulk edit section
  const getBulkEdit = () => app.find(BulkEdit);

  const getCorrectionReasonSelect = () => getBulkEdit().find(Select).last();
  const getCorrectionReasonOptions = () => getCorrectionReasonSelect().props().options;

  const getQuantumEditorComponent = () => getBulkEdit().find(QuantumEditor);
  const getQuantumTextInput = () => getQuantumEditorComponent().find(TextInput);
  const getQuantumCorrectionReasonSelect = () => getQuantumEditorComponent().find(Select).last();
  const getQuantumAdjustButton = () => getQuantumEditorComponent().find(Button).first();

  const getStockCoverEditorComponent = () => getBulkEdit().find(StockCoverEditor);
  const getStockCoverSelect = () => getStockCoverEditorComponent().find(Select).first();
  const getStockCoverOptions = () => getStockCoverSelect().props().options;
  const getStockCoverCorrectionReasonSelect = () => getStockCoverEditorComponent().find(Select).last();
  const getStockCoverAdjustButton = () => getStockCoverEditorComponent().find(Button).first();

  const getSelectionEditorComponent = () => getBulkEdit().find(SelectionEditor);
  const getAdjustManualReviewButton = () => getSelectionEditorComponent().find(Button).last();

  const getTransitEditorComponent = () => getBulkEdit().find(TransitEditor);
  const getTransitEditorSubmitButton = () => getTransitEditorComponent().find(Button);

  it('Should render correctly', () => {
    expect(getBulkEdit()).toHaveLength(1);
    expect(getQuantumEditorComponent()).toHaveLength(1);
    expect(api.recommendationController.replenishmentWithCountPost).toBeCalledWith(
      {
        filter: {
          demandIds: DEMAND_IDS,
          filter: FILTER,
        },
      },
      DemandType.TYPE_1P
    );
  });

  it('Should adjust by quantum', async () => {
    const filterOption = getFilterOptions()[3];
    expect(filterOption.value.id).toEqual(FilterType.NEW);

    await act(async () => {
      await getFilterSelector().props().onChange(filterOption);
      app.update();

      await getQuantumTextInput()
        .props()
        .onChange({ target: { value: '5' } });
      getQuantumCorrectionReasonSelect().props().onChange(getCorrectionReasonOptions()[3]);
      getQuantumAdjustButton().simulate('click');
    });

    expect(api.recommendationController.ebpAdjustQuantity).toBeCalledWith(
      {
        correctionReason: 4,
        quantums: 5,
        demandKeys: DEMAND_KEYS,
        recommendationFilters: {
          filter: {
            ids: [],
            demandIds: DEMAND_IDS,
            filter: FILTER,
            weeks: WEEKS,
          },
          userFilter: USER_FILTER,
        },
      },
      DemandType.TYPE_1P
    );
  });

  it('Should adjust by stock cover with default values', async () => {
    const filterOption = getFilterOptions()[6];
    expect(filterOption.value.id).toEqual(FilterType.SC);

    await act(async () => {
      await getFilterSelector().props().onChange(filterOption);
      app.update();

      expect(getStockCoverAdjustButton().prop('disabled')).toBeFalsy();
      getStockCoverCorrectionReasonSelect().props().onChange(getCorrectionReasonOptions()[4]);
      getStockCoverAdjustButton().simulate('click');
    });

    expect(getStockCoverAdjustButton().prop('disabled')).toBeTruthy();
    expect(api.recommendationController.ebpAdjustQuantity).toBeCalledWith(
      {
        correctionReason: 5,
        makeStockCoverEqualsWeeks: 2,
        demandKeys: DEMAND_KEYS,
        recommendationFilters: {
          filter: {
            ids: [],
            demandIds: DEMAND_IDS,
            filter: FilterType.SC,
            weeks: WeeksType.ZERO_TWO,
          },
          userFilter: USER_FILTER,
        },
        stockCoverType: StockCoverType.BACKWARD,
      },
      DemandType.TYPE_1P
    );
  });

  it('Should adjust by stock cover with custom values', async () => {
    const filterOption = getFilterOptions()[6];
    expect(filterOption.value.id).toEqual(FilterType.SC);

    await act(async () => {
      await getFilterSelector().props().onChange(filterOption);
      app.update();

      expect(getStockCoverAdjustButton().prop('disabled')).toBeFalsy();
      getStockCoverSelect().props().onChange(getStockCoverOptions()[1]);
      app.update();

      getStockCoverCorrectionReasonSelect().props().onChange(getCorrectionReasonOptions()[4]);
      getStockCoverAdjustButton().simulate('click');
    });

    expect(getStockCoverAdjustButton().prop('disabled')).toBeTruthy();
    expect(api.recommendationController.ebpAdjustQuantity).toBeCalledWith(
      {
        correctionReason: 5,
        makeStockCoverEqualsWeeks: 3,
        demandKeys: DEMAND_KEYS,
        recommendationFilters: {
          filter: {
            ids: [],
            demandIds: DEMAND_IDS,
            filter: FilterType.SC,
            weeks: WeeksType.ZERO_TWO,
          },
          userFilter: USER_FILTER,
        },
        stockCoverType: StockCoverType.BACKWARD,
      },
      DemandType.TYPE_1P
    );
  });

  it('Should adjust needs manual review', async () => {
    getAdjustManualReviewButton().simulate('click');
    app.update();

    expect(api.recommendationController.ebpAdjustNeedsManualReview).toBeCalledWith(
      {
        needsManualReview: false,
        demandKeys: DEMAND_KEYS,
        recommendationFilters: {
          filter: {
            ids: [],
            demandIds: DEMAND_IDS,
            filter: FILTER,
            weeks: WEEKS,
          },
          userFilter: USER_FILTER,
        },
      },
      DemandType.TYPE_1P
    );
  });

  it('Should set transits to zero', () => {
    getTransitEditorSubmitButton().simulate('click');
    app.update();

    expect(api.recommendationController.ebpAdjustTransit).toBeCalledWith(
      {
        transit: 0,
        demandKeys: DEMAND_KEYS,
        recommendationFilters: {
          filter: {
            ids: [],
            demandIds: DEMAND_IDS,
            filter: FilterType.NEW,
            weeks: undefined,
          },
          userFilter: USER_FILTER,
        },
      },
      DemandType.TYPE_1P
    );
  });
});
