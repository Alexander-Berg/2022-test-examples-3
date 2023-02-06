'use strict';

import {makeElementInvisibleBySelector} from 'spec/gemini/helpers';
import UserSnippet from 'spec/page-objects/UserSnippet';
import LayoutBase from 'spec/page-objects/LayoutBase';
import PopupB2b from 'spec/page-objects/PopupB2b';
import User from 'spec/page-objects/User';
import {Case} from 'spec/gemini/lib/types';

export default {
    suiteName: 'Multipassport',
    selector: PopupB2b.activeBodyPopup,
    before(actions) {
        actions.waitForElementToShow(User.root, 10000);
    },
    capture(actions, find) {
        actions
            .click(find(`${User.root} ${UserSnippet.root}`))
            .waitForElementToShow(PopupB2b.activeBodyPopup, 5000)
            .wait(5000);
        makeElementInvisibleBySelector.call(actions, LayoutBase.root);
    },
} as Case;
