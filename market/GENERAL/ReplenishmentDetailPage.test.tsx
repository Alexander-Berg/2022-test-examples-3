import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { ReactWrapper } from 'enzyme';
import { always, of, T } from 'ramda';
import { act } from '@testing-library/react';

import { DateEdit, DirectDemandActions } from './components/DemandInfo/components';
import { recommendationsCount } from 'src/test/data/recomendationsCount';
import { ReplenishmentDetailPage } from './ReplenishmentDetailPage';
import { createRecommendation } from 'src/test/data/recomendations';
import { correctionReasons } from 'src/test/data/correctionReasons';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { DemandType } from 'src/java/definitions-replenishment';
import { createDemandDTO } from 'src/test/data/demand';
import { setupTestApp } from 'src/test/setupApp';
import { Attach, Button } from 'src/components';
import Api from 'src/Api';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

const DEMAND_ID = 1337;
const DEMAND_VERSION = 1;

const DEMAND_MOCK = createDemandDTO({
  id: DEMAND_ID,
  version: DEMAND_VERSION,
  supplier: suppliers[1],
  orderId: 'testOrderId',
});

const RECOMMENDATIONS_MOCK = [
  createRecommendation({ msku: 1, setQuantity: 1, vendorId: 111, correctionReason: { name: '', id: 0 } }),
  createRecommendation({ msku: 2, setQuantity: 1, vendorId: 111, correctionReason: { name: '', id: 0 } }),
  createRecommendation({ msku: 3, setQuantity: 1, vendorId: 222, correctionReason: { name: '', id: 0 } }),
];

const alwaysVoid = always(undefined);

describe('<ReplenishmentDetailPage />', () => {
  jest.spyOn(window, 'scrollTo').mockImplementation(alwaysVoid);
  jest.spyOn(window, 'confirm').mockImplementation(T);

  beforeEach(() => {
    ({ app, api } = setupTestApp(`/replenishment/ebp?demandIds=${DEMAND_ID}`));

    api.demandController.demands.next().resolve(of(DEMAND_MOCK));
    api.recommendationController.replenishmentWithCountPost.next().resolve({
      count: recommendationsCount,
      userFiltersCount: [],
      numberOfCorrectedRecommendations: 0,
      recommendations: RECOMMENDATIONS_MOCK,
    });

    app.update();

    api.correctionReasonController.getCorrectionReasons.next().resolve(correctionReasons);
    app.update();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const getReplenishmentDetailPage = () => app.find(ReplenishmentDetailPage);

  it('Should mount correctly', () => {
    expect(getReplenishmentDetailPage()).toHaveLength(1);
    expect(api.demandController.demandsToUnite).toBeCalledWith(DEMAND_ID, DemandType.TYPE_1P);
    expect(api.demandController.demands).toBeCalledWith({
      id: of(DEMAND_ID),
      demandType: DemandType.TYPE_1P,
    });
  });

  it('Should upload excel', () => {
    const file = new File(['(⌐□_□)'], 'myFile.xlsx', { type: 'application/vnd.ms-excel' });
    getReplenishmentDetailPage()
      .find(Attach)
      .find('input')
      .simulate('change', {
        target: {
          files: of(file),
          value: '',
        },
      });

    expect(api.recommendationController.uploadExcel2).toBeCalledWith(file, DEMAND_VERSION, DemandType.TYPE_1P);
  });

  it('Should edit delivery date', () => {
    const deliveryDate = getReplenishmentDetailPage().find(DateEdit);
    expect(deliveryDate).toHaveLength(1);

    const editLink = deliveryDate.find('a');
    expect(editLink).toHaveLength(1);
    editLink.simulate('click');
    app.update();

    const editingContainer = getReplenishmentDetailPage().find(DateEdit);
    expect(editingContainer).toHaveLength(1);

    const inputField = editingContainer.find('input');
    expect(inputField).toHaveLength(1);
    inputField.simulate('change', { target: { value: '2020-10-10' } });
    app.update();

    const saveButton = editingContainer.find('button').last();
    expect(saveButton).toHaveLength(1);
    saveButton.simulate('click');
    app.update();

    expect(api.demandController.editDeliveryDate).toBeCalledWith(
      1337,
      { deliveryDate: '2020-10-10' },
      DemandType.TYPE_1P
    );
  });

  it('Should export recommendations', async () => {
    // Пометить как обработанную и не отправлять в AX
    const buttons = app.find(DirectDemandActions).find(Button);
    expect(buttons).toHaveLength(3);

    const exportBtn = buttons.at(1);
    expect(exportBtn.text()).toBe('Отправить в AX');

    await act(() => {
      exportBtn.simulate('click');
      app.update();
    });

    await act(() => {
      api.recommendationController.recommendationsWithEditWarnings.next().resolve({
        recommendations: [],
        warningCounts: {} as any,
      });
      app.update();
    });

    await act(() => {
      api.groupParamController.getBySuppliers.next().resolve([]);
      app.update();
    });

    expect(window.confirm).toBeCalledTimes(1);
    expect(api.demandController.saveDemandsForExport).toBeCalledTimes(1);
    expect(api.demandController.saveDemandsForExport).toBeCalledWith(
      of({
        id: DEMAND_ID,
        version: DEMAND_VERSION,
      }),
      DemandType.TYPE_1P
    );
  });

  it('Should mark demand as exported', async () => {
    const buttons = app.find(DirectDemandActions).find(Button);
    expect(buttons).toHaveLength(3);

    const markingBtn = buttons.at(2);
    expect(markingBtn.text()).toBe('Пометить как обработанную и не отправлять в AX');

    await act(() => {
      markingBtn.simulate('click');
      app.update();
    });

    await act(() => {
      api.groupParamController.getBySuppliers.next().resolve([]);
      app.update();
    });

    expect(window.confirm).toBeCalledTimes(1);
    expect(api.demandController.markAsHandled).toBeCalledTimes(1);
    expect(api.demandController.markAsHandled).toBeCalledWith(
      DemandType.TYPE_1P,
      of({
        id: DEMAND_ID,
        version: DEMAND_VERSION,
      })
    );
  });
});
