import { Category } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { Scope } from '@yandex-market/market-proto-dts/Market/Mboc/ContentCommentTypes';
import { getNormalizedCategoryData } from '@yandex-market/mbo-parameter-editor/es';
import { mount } from 'enzyme';
import equals from 'ramda/es/equals';
import React from 'react';
import { Provider } from 'react-redux';

import { RUSSIAN_LANG_ID } from 'src/shared/constants';
import { DEFAULT_CATEGORY_ID } from 'src/shared/test-data/test-constants';
import { testModelProto } from 'src/shared/test-data/test-models';
import { IsLoading } from 'src/shared/utils/loadable';
import { wait } from 'src/shared/utils/testing/utils';
import { ModerationSceneLoader } from '../../components/ModerationSceneLoader/ModerationSceneLoader';
import contentCommentsActions from 'src/tasks/mapping-moderation/store/contentComments/contentCommentsActions';
import { setupTestStore } from 'src/tasks/mapping-moderation/store/mocks/store';
import offersActions from 'src/tasks/mapping-moderation/store/offers/offersActions';
import { testModerationOfferInput } from 'src/tasks/mapping-moderation/test-data/test-moderation-offer-input';

describe('Store', () => {
  it(`shouldn't fall on very empty offer without data`, () => {
    const { store, aliasMaker } = setupTestStore();

    store.dispatch(offersActions.init({ offers: [testModerationOfferInput({ id: '1' })] }));

    // offer without data shouldn't request anything even models - there are no models to request
    expect(aliasMaker.activeRequests().map(r => r.name)).toEqual(['getTaskOffers']);
  });

  it('should initialize right, given some offers', async () => {
    const { aliasMaker, store } = setupTestStore();

    store.dispatch(contentCommentsActions.loadTypes.started({ scope: Scope.MODERATION }));
    store.dispatch(
      offersActions.init({
        offers: [
          testModerationOfferInput({ id: '1', offer_id: '101' }),
          testModerationOfferInput({ id: '2', offer_id: '102' }),
        ],
      })
    );

    // testModerationOffer({ id: '1', targetSkuId: 1001 }),
    // testModerationOffer({ id: '2', targetSkuId: 1002 }),
    expect(aliasMaker.activeRequests().map(r => r.name)).toIncludeSameMembers([
      'getTaskOffers',
      'getContentCommentTypes',
    ]);

    aliasMaker.getTaskOffers.next().resolve({
      offer: [
        { offer_id: '101', supplier_mapping_info: { sku_id: 1001 } },
        { offer_id: '102', supplier_mapping_info: { sku_id: 1002 } },
      ],
    });

    await wait(1);

    aliasMaker.getContentCommentTypes.next().resolve({
      response: {
        content_comment_type: [
          {
            scope: [Scope.MATCHING],
            allow_other: 'Другое',
            description: 'Чего-то тут не хватает',
            variant: [{ name: 'Или того' }, { name: 'Или этого' }],
          },
        ],
      },
    });

    const ANOTHER_CATEGORY = 1002;
    aliasMaker.findModels
      .next(r => equals(r.model_ids, [1001, 1002]))
      .resolve({
        model: [testModelProto({ id: 1001 }), testModelProto({ id: 1002, categoryId: ANOTHER_CATEGORY })],
      });
    expect(aliasMaker.activeRequests().map(r => r.name)).toIncludeSameMembers([
      'getModelsExportedCached',
      'getModelForms',
      'getParameters',
    ]);

    const defaultCategory: Category = {
      hid: DEFAULT_CATEGORY_ID,
      name: [{ name: 'Test', lang_id: RUSSIAN_LANG_ID }],
      parameter: [],
    };
    const defaultCategoryUi = getNormalizedCategoryData(defaultCategory);

    aliasMaker.getParameters
      .next(r => r.category_id === DEFAULT_CATEGORY_ID)
      .resolve({
        response: {
          category_parameters: defaultCategory,
        },
      });

    expect(store.getState().categories).toEqual({
      [DEFAULT_CATEGORY_ID]: defaultCategoryUi,
      [ANOTHER_CATEGORY]: IsLoading,
    });

    aliasMaker.getModelForms
      .next(r => equals(r.category_ids, [DEFAULT_CATEGORY_ID, ANOTHER_CATEGORY]))
      .resolve({
        response: {
          model_forms: [
            { category_id: DEFAULT_CATEGORY_ID, published: true, form_tabs: [] },
            { category_id: ANOTHER_CATEGORY, published: true, form_tabs: [] },
          ],
        },
      });

    // Now it should be loaded
    expect(store.getState().categories[DEFAULT_CATEGORY_ID]).not.toEqual(IsLoading);
    expect(store.getState().categories[ANOTHER_CATEGORY]).toEqual(IsLoading);

    // Try to render it
    mount(
      <Provider store={store}>
        <ModerationSceneLoader />
      </Provider>
    );
  });
});
