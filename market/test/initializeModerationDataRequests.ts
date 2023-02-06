import { Offer } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { Model as ProtoModel } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { Category as ProtoCategory } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { ReactWrapper, ShallowWrapper } from 'enzyme';
import { MockedApi } from '@yandex-market/mbo-test-utils';

import { AliasMaker } from 'src/shared/services';
import { testFormProto } from 'src/shared/test-data/test-categories';
import { equalsIgnoreOrder } from 'src/shared/utils/ramda-helpers';
import { wait } from 'src/shared/utils/testing/utils';

export default async function initializeModerationDataRequests(
  aliasMaker: MockedApi<AliasMaker>,
  category: ProtoCategory,
  skus: ProtoModel[],
  wrapper: ShallowWrapper | ReactWrapper,
  offers?: Offer[]
) {
  // optional: check component is rendered fine in intermediate states (this is common loading sequence)
  const renderComponent = () => wrapper.update();

  aliasMaker.getContentCommentTypes.next().resolve({ response: { content_comment_type: [] } });
  renderComponent();

  if (offers) {
    aliasMaker.getTaskOffers.next().resolve({ offer: offers });
  }

  await wait(1);

  aliasMaker.findModels
    .next(r =>
      equalsIgnoreOrder(
        r.model_ids || [],
        skus.map(s => s.id)
      )
    )
    .resolve({ model: skus });
  renderComponent();

  aliasMaker.getModelsExportedCached
    .next(r =>
      equalsIgnoreOrder(
        r.model_id as number[],
        skus.map(s => s.id)
      )
    )
    .resolve({ model: skus });
  renderComponent();

  aliasMaker.getModelForms
    .next(r => equalsIgnoreOrder(r.category_ids || [], [category.hid]))
    .resolve({ response: { model_forms: [testFormProto(category)] } });
  renderComponent();

  aliasMaker.getParameters
    .next(r => r.category_id === category.hid)
    .resolve({ response: { category_parameters: category } });
  renderComponent();

  expect(aliasMaker.activeRequests()).toBeEmpty();
}
