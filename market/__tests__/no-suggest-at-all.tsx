import { ToastContainer } from 'react-toastify';

import { testCategoryProto } from 'src/shared/test-data/test-categories';
import { testModelProto } from 'src/shared/test-data/test-models';
import { wait } from 'src/shared/utils/testing/utils';
import { ModerationTaskType } from 'src/tasks/mapping-moderation/helpers/input-output';
import initializeModerationDataRequests from 'src/tasks/mapping-moderation/helpers/test/initializeModerationDataRequests';
import { setupTestApplication } from 'src/tasks/mapping-moderation/helpers/test/setupTestApplication';

describe('ModerationApp', () => {
  it('Should display errors for absent suggest mapping', async () => {
    const category = { ...testCategoryProto() };
    const sku = testModelProto({ category });

    const wrongOffer = { id: 1 } as any;
    const goodOffer = { id: 2, generated_sku_id: sku.id } as any;
    const { aliasMaker, app } = setupTestApplication({
      offers: [wrongOffer, goodOffer],
      task_type: ModerationTaskType.MAPPING_MODERATION,
    });
    await initializeModerationDataRequests(aliasMaker, category, [sku], app);

    await wait(1);
    app.update();

    const error = app.find(ToastContainer);
    expect(error.text()).toContain('нет ни targetSkuId ни generatedSkuId');
    expect(error.text()).toContain(String(wrongOffer.id));
  });
});
