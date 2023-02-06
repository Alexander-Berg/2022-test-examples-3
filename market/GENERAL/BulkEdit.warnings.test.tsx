import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { Tab } from '@yandex-market/mbo-components';
import { act } from 'react-dom/test-utils';
import { ReactWrapper } from 'enzyme';

import { QuantumEditor, SelectionEditor, StockCoverEditor, TransitEditor } from './components';
import { DemandType, StockCoverType, WarningFilterType } from 'src/java/definitions-replenishment';
import { recommendationsCount } from 'src/test/data/recomendationsCount';
import { Button, CheckBox, Select, TextInput } from 'src/components';
import { createRecommendation } from 'src/test/data/recomendations';
import { correctionReasons } from 'src/test/data/correctionReasons';
import { ReplenishmentFilter } from '../ReplenishmentFilter';
import { createDemandDTO } from 'src/test/data/demand';
import { FilterContainer } from '../FilterContainer';
import { WarningsFilter } from '../WarningsFilter';
import { setupTestApp } from 'src/test/setupApp';
import { BulkEdit } from './BulkEdit';
import Api from 'src/Api';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const DEMAND_ID_1 = 1111;
const DEMAND_ID_2 = 2222;

const DEMAND_IDS = [DEMAND_ID_1, DEMAND_ID_2];
const DEMAND_KEYS = DEMAND_IDS.map(id => ({ id, version: 1 }));

const RECOMMENDATION_ID_1 = 11;
const RECOMMENDATION_ID_2 = 12;

const RECOMMENDATIONS_MOCK = [
  createRecommendation({
    id: RECOMMENDATION_ID_1,
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
    id: RECOMMENDATION_ID_2,
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

const WARNING_COUNTS_MOCK = {
  ADJUSTED_PURCH_QTY_OVER_PURCH_QTY: 1,
  SCB_OVER_MAX_LIFETIME: 1,
  SCF_OVER_MAX_LIFETIME: 0,
};

describe('<BulkEdit />', () => {
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

    api.demandController.getWarningCounts.next().resolve(WARNING_COUNTS_MOCK);

    api.correctionReasonController.getCorrectionReasons.next().resolve(correctionReasons);
    app.update();
  });

  // Filter section
  const getFilterContainer = () => app.find(FilterContainer);
  const getFilterContainerTabs = () => getFilterContainer().find(Tab);

  // Warning filters
  const getWarningsFilter = () => getFilterContainer().find(WarningsFilter);
  const getWarningFilterOptions = () => getWarningsFilter().find(CheckBox);

  // Normal filters
  const getReplenishmentFilter = () => getFilterContainer().find(ReplenishmentFilter);

  // Bulk edit section
  const getBulkEdit = () => getFilterContainer().find(BulkEdit);

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
  const getTransitEditorComponent = () => getBulkEdit().find(TransitEditor);

  function setWarningTab() {
    const tabs = getFilterContainerTabs();
    expect(tabs).toHaveLength(2);

    // Default state, normal filters
    expect(getReplenishmentFilter()).toHaveLength(1);
    expect(getWarningsFilter()).toHaveLength(0);

    act(() => {
      tabs.at(1).props().onClick();
    });

    app.update();

    // Switched to warning filters
    expect(getReplenishmentFilter()).toHaveLength(0);
    expect(getWarningsFilter()).toHaveLength(1);

    // Only non-zero filter options should be rendered
    expect(getWarningFilterOptions()).toHaveLength(2);
  }

  it('Should render correctly', () => {
    expect(getFilterContainer()).toHaveLength(1);
    setWarningTab();

    expect(getBulkEdit()).toHaveLength(1);

    // При редактировании подозрительных рекомендаций доступны только кванты и SC
    expect(getQuantumEditorComponent()).toHaveLength(1);
    expect(getStockCoverEditorComponent()).toHaveLength(1);

    // NeedsManualReview и транзины недоступны
    expect(getSelectionEditorComponent()).toHaveLength(0);
    expect(getTransitEditorComponent()).toHaveLength(0);

    expect(api.recommendationController.replenishmentWithCountPost).toBeCalled();
  });

  function setWarningFilter(index: 0 | 1) {
    setWarningTab();

    expect(api.recommendationController.recommendationsWithEditWarnings).toBeCalledWith(
      {
        demandIds: DEMAND_IDS,
        warnings: [],
      },
      DemandType.TYPE_1P
    );

    api.recommendationController.recommendationsWithEditWarnings.next().resolve({
      recommendations: RECOMMENDATIONS_MOCK,
      warningCounts: WARNING_COUNTS_MOCK,
    });

    act(() => {
      getWarningFilterOptions().at(index).props().onChange();
    });

    expect(api.recommendationController.recommendationsWithEditWarnings).toBeCalledWith(
      {
        demandIds: DEMAND_IDS,
        warnings: index
          ? [WarningFilterType.SCB_OVER_MAX_LIFETIME]
          : [WarningFilterType.ADJUSTED_PURCH_QTY_OVER_PURCH_QTY],
      },
      DemandType.TYPE_1P
    );

    api.recommendationController.recommendationsWithEditWarnings.next().resolve({
      recommendations: RECOMMENDATIONS_MOCK.slice(index, index + 1),
      warningCounts: WARNING_COUNTS_MOCK,
    });

    app.update();
  }

  it('Should adjust by quantum', async () => {
    setWarningFilter(0);

    act(() => {
      getQuantumTextInput()
        .props()
        .onChange({ target: { value: '5' } });
      getQuantumCorrectionReasonSelect().props().onChange(getCorrectionReasonOptions()[3]);
    });

    app.update();

    act(() => {
      getQuantumAdjustButton().props().onClick();
    });

    expect(api.recommendationController.ebpAdjustQuantity).toBeCalledWith(
      {
        correctionReason: 4,
        quantums: 5,
        demandKeys: DEMAND_KEYS,
        recommendationFilters: {
          filter: {
            ids: [RECOMMENDATION_ID_1],
            demandIds: [DEMAND_ID_1],
          },
          userFilter: [],
        },
      },
      DemandType.TYPE_1P
    );
  });

  it('Should adjust by stock cover with default values', () => {
    setWarningFilter(0);

    expect(getStockCoverAdjustButton().prop('disabled')).toBeFalsy();

    act(() => {
      getStockCoverCorrectionReasonSelect().props().onChange(getCorrectionReasonOptions()[4]);
    });

    app.update();

    act(() => {
      getStockCoverAdjustButton().props().onClick();
    });

    app.update();

    expect(getStockCoverAdjustButton().prop('disabled')).toBeTruthy();
    expect(api.recommendationController.ebpAdjustQuantity).toBeCalledWith(
      {
        correctionReason: 5,
        makeStockCoverEqualsWeeks: 2,
        demandKeys: DEMAND_KEYS,
        recommendationFilters: {
          filter: {
            ids: [RECOMMENDATION_ID_1],
            demandIds: [DEMAND_ID_1],
          },
          userFilter: [],
        },
        stockCoverType: StockCoverType.BACKWARD,
      },
      DemandType.TYPE_1P
    );
  });

  it('Should adjust by stock cover with custom values', () => {
    setWarningFilter(1);

    expect(getStockCoverAdjustButton().prop('disabled')).toBeFalsy();

    act(() => {
      getStockCoverSelect().props().onChange(getStockCoverOptions()[1]);
      getStockCoverCorrectionReasonSelect().props().onChange(getCorrectionReasonOptions()[4]);
    });

    app.update();

    act(() => {
      getStockCoverAdjustButton().props().onClick();
    });

    app.update();

    expect(getStockCoverAdjustButton().prop('disabled')).toBeTruthy();
    expect(api.recommendationController.ebpAdjustQuantity).toBeCalledWith(
      {
        correctionReason: 5,
        makeStockCoverEqualsWeeks: 3,
        demandKeys: DEMAND_KEYS,
        recommendationFilters: {
          filter: {
            ids: [RECOMMENDATION_ID_2],
            demandIds: [DEMAND_ID_2],
          },
          userFilter: [],
        },
        stockCoverType: StockCoverType.BACKWARD,
      },
      DemandType.TYPE_1P
    );
  });
});
