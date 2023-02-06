import { ModelType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { equalsIgnoreOrder } from 'src/shared/utils/ramda-helpers';
import { setupTestStore } from 'src/tasks/mapping-moderation/store/mocks/store';
import modelsActions from 'src/tasks/mapping-moderation/store/models/modelsActions';

describe('Models store', () => {
  it('Should produce errors for not loaded models', () => {
    const { aliasMaker, store } = setupTestStore();
    store.dispatch(modelsActions.load.started({ ids: [42] }));
    aliasMaker.findModels.next(r => equalsIgnoreOrder(r.model_ids || [], [42])).resolve({ model: [] });
  });

  it('Should produce errors for not loaded exported models', () => {
    const { aliasMaker, store } = setupTestStore();
    store.dispatch(modelsActions.loadExported.started({ ids: [42], categoryId: 42, type: ModelType.SKU }));
    aliasMaker.getModelsExportedCached
      .next(r => equalsIgnoreOrder(r.model_id as number[], [42]))
      .resolve({ model: [] });
  });
});
