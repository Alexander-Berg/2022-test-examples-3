import { mount } from 'enzyme';
import * as React from 'react';
import { createApiMock } from '@yandex-market/mbo-test-utils';

import { AliasMaker } from 'src/shared/services';
import { testOffer } from 'src/shared/test-data/test-offers';
import { equalsIgnoreOrder } from 'src/shared/utils/ramda-helpers';
import { wait } from 'src/shared/utils/testing/utils';
import { ModerationApp } from 'src/tasks/mapping-moderation/components/ModerationApp/ModerationApp';
import { MappingModerationInput } from 'src/tasks/mapping-moderation/helpers/input-output';

describe('ModerationApp', () => {
  it('Should display error when msku not found in response', async () => {
    const aliasMaker = createApiMock<AliasMaker>();
    const input: MappingModerationInput = {
      offers: [testOffer({ id: 'testOfferId', supplierSkuId: 10 }) as any],
    };
    const app = mount(<ModerationApp input={input} onSubmit={() => undefined} aliasMaker={aliasMaker} />);

    aliasMaker.getTaskOffers.next().resolve({ offer: input.offers as any });

    await wait(1);

    aliasMaker.findModels.next(r => equalsIgnoreOrder(r.model_ids || [], [10])).resolve({ model: [] }); // empty response

    await wait(1);
    app.update();
  });
});
