'use strict';

import initialState from 'spec/lib/page-mocks/modelsPromotion.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import PopupB2b from 'spec/page-objects/PopupB2b';
import InputB2b from 'spec/page-objects/InputB2b';
import ModelsPromotionList from 'spec/page-objects/ModelsPromotionList';
import {makeElementInvisibleBySelector} from 'spec/gemini/helpers';
import {User} from 'spec/lib/constants/users/users';

const bidsHint = PopupB2b.activeBodyPopup;
const bidsInput = `${ModelsPromotionList.getRow(0)} ${InputB2b.root}`;

type Options = {
    user: User;
    url: string;
};

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Bids hint',
        selector: bidsHint,
        state: {
            vendorsModelsPromotion: initialState,
        },
        before(actions, find) {
            actions
                .waitForElementToShow(bidsInput, 10000)
                .click(find(bidsInput))
                .waitForElementToShow(bidsHint, 10000)
                .wait(5000);
            makeElementInvisibleBySelector.call(actions, ModelsPromotionList.modelListHeader);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
