import React, { FC } from 'react';
import { fireEvent, render } from '@testing-library/react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { BatchStatusChangesDialog } from '.';
import { AvailabilityContext, AvailabilityContextValue } from 'src/pages/SskuAvailabilityPages/context';
import { OfferAvailability } from 'src/java/definitions';

const store = createStore(rootReducer);

const contextValue: AvailabilityContextValue = {
  makeShopSkuByIdSelector: jest.fn(),
  makeGetChangedAvailabilitySelector: jest.fn(),
  makeCheckAvailabilityIsChoosedSelector: jest.fn(),
  makeCheckAllWarehouseAvailabilitiesAreChoosedSelector: jest.fn(),
  getBatchChangesSelector: jest.fn(),
  getChangedAvailabilitiesSelector: jest.fn(),
  getChoosedAvailabilitiesSelector: jest.fn(),
  getFilteredSskuCountSelector: jest.fn(() => 100),
  getCategoryLink: jest.fn(),
  makeGetChangedStatusSelector: jest.fn(),
  getBatchStatusChangesSelector: jest.fn(() => ({
    availability: OfferAvailability.DELISTED,
    isChoosed: true,
  })),
  handleWarehouseMatrixAudit: jest.fn(),
  handleBPSelect: jest.fn(),
};
const Wrapper: FC = ({ children }) => {
  return (
    <Provider store={store}>
      <AvailabilityContext.Provider value={contextValue}>{children}</AvailabilityContext.Provider>
    </Provider>
  );
};

describe('BatchStatusChangesDialog', () => {
  it('main flow', () => {
    const onSave = jest.fn();
    const onDiscard = jest.fn();
    const component = render(
      <Wrapper>
        <BatchStatusChangesDialog
          isBatchStatusChangesValid
          onSaveBatchStatusChanges={onSave}
          onDiscardBatchStatusChanges={onDiscard}
          changesCount={0}
          isBadStatusChanges={false}
        />
      </Wrapper>
    );

    fireEvent.click(component.getByRole('button', { name: 'Сохранить' }));
    expect(onSave).toHaveBeenCalledTimes(1);
    fireEvent.click(component.getByRole('button', { name: 'Отменить' }));
    expect(onDiscard).toHaveBeenCalledTimes(1);
  });
});
