import React from 'react';
import { act, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupWithReatom } from 'src/test/withReatom';
import { shopModel, parameter } from 'src/test/data';
import { resolveSaveModelsRequest } from 'src/test/api/resolves';
import { shopModelsAtom, setAllShopModelsAction, shopModelChangesAtom, setAuditItemAction } from 'src/store/shopModels';

import { UndoChangesPane } from './UndoChangesPane';

const changedItem = {
  id: 1,
  shopModelId: shopModel.id,
  oldValues: [],
  newValues: [],
  parameterId: parameter.id,
  canHotReset: true,
  changeDate: new Date(),
};
describe('UndoChangesPane', () => {
  test('close', async () => {
    const { app, reatomStore } = setupWithReatom(<UndoChangesPane />, { shopModelChangesAtom }, []);

    act(() => {
      // добавляем новые изменения
      reatomStore.dispatch(setAuditItemAction([changedItem]));
    });

    // ждем когда они прикатят в стейт
    await waitFor(() =>
      expect(reatomStore.getState(shopModelChangesAtom).filter(el => el.canHotReset)).toHaveLength(1)
    );

    const close = await app.findByTitle(new RegExp('Закрыть', 'i'));
    userEvent.click(close);

    await waitFor(() =>
      expect(reatomStore.getState(shopModelChangesAtom).filter(el => el.canHotReset)).toHaveLength(0)
    );
  });

  test('discard changes', async () => {
    const { app, api, reatomStore } = setupWithReatom(<UndoChangesPane />, { shopModelChangesAtom, shopModelsAtom }, [
      setAllShopModelsAction([shopModel]),
    ]);

    act(() => {
      reatomStore.dispatch(setAuditItemAction([changedItem]));
    });

    const cancel = await app.findByText(new RegExp('Отменить', 'i'));
    userEvent.click(cancel);

    resolveSaveModelsRequest(api, [shopModel]);
  });
});
