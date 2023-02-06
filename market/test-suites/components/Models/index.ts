'use strict';

import initialState from 'spec/lib/page-mocks/modelsPromotion.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import Filters from 'spec/page-objects/Filters';
import ModelsPromotionListItem from 'spec/page-objects/ModelsPromotionListItem';
import {User} from 'spec/lib/constants/users/users';

type Options = {user: User; url: string};

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Models-page',
        selector: [ModelsPromotionListItem.root, Filters.root],
        state: {
            vendorsModelsPromotion: initialState,
        },
        before(actions) {
            actions
                .waitForElementToShow(ModelsPromotionListItem.root, 10000)
                .waitForElementToShow(Filters.root, 1000)
                .wait(2000);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
