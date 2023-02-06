import React, { FC } from 'react';
import { fireEvent, render } from '@testing-library/react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { StatusChangesDialog } from '.';
import { PageContext, PageContextValue } from 'src/pages/MskuAvailabilityPages/context';
import { updateBatchStatusChangeAction } from 'src/store/root/market-sku-fulfillment/market-sku-fulfillment.actions';
import { ActionStatus, DisplayMskuStatusValue, WarehouseUsingType } from 'src/java/definitions';
import { exportMskuTableToExcel } from 'src/store/root/market-sku/market-sku.actions';

const store = createStore(rootReducer);

const pageContextValue: PageContextValue = {
  usingType: WarehouseUsingType.USE_FOR_FULFILLMENT,
  exportTableAction: exportMskuTableToExcel.fulfilment,
  makeMarketSkuByIdSelector: jest.fn(),
  makeGetChangedAvailabilitySelector: jest.fn(),
  makeCheckAvailabilityIsChoosedSelector: jest.fn(),
  makeCheckAllWarehouseAvailabilitiesAreChoosedSelector: jest.fn(),
  getBatchChangesSelector: jest.fn(),
  getChangedAvailabilitiesSelector: jest.fn(),
  getChoosedAvailabilitiesSelector: jest.fn(),
  getFilteredMskuCountSelector: jest.fn(() => 100),
  getCategoryLink: jest.fn(),
  handleWarehouseMatrixAudit: jest.fn(),
};

const Wrapper: FC = ({ children }) => {
  return (
    <Provider store={store}>
      <PageContext.Provider value={pageContextValue}>{children}</PageContext.Provider>
    </Provider>
  );
};

describe('BatchStatusChangesDialog', () => {
  it('main flow', () => {
    const component = render(
      <Wrapper>
        <StatusChangesDialog />
      </Wrapper>
    );

    store.dispatch(
      updateBatchStatusChangeAction({
        isChoosed: true,
        statusChange: {
          newStatus: DisplayMskuStatusValue.REGULAR,
          isEditMode: false,
        },
      })
    );

    fireEvent.click(component.getByRole('button', { name: 'Сохранить' }));
    expect(store.getState().marketSkuFulfillment.batchChangesSavingStatus.status).toEqual(ActionStatus.IN_PROGRESS);
    fireEvent.click(component.getByRole('button', { name: 'Отменить' }));
    expect(store.getState().marketSkuFulfillment.batchStatusChanges.isChoosed).toBeFalsy();
  });
});
