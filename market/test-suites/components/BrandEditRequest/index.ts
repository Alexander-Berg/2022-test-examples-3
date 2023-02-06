'use strict';

import initialState from 'spec/lib/page-mocks/moderation.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import ModerationList from 'spec/page-objects/ModerationList';
import {User} from 'spec/lib/constants/users/users';

type Options = {
    user: User;
    url: string;
};

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Brand Edit Request',
        selector: ModerationList.item,
        state: {
            vendorsBrandEditRequests: initialState,
        },
        before(actions, find) {
            actions
                .waitForElementToShow(ModerationList.getItemSelectorByIndex(), 10000)
                .click(find(ModerationList.getItemSelectorByIndex()))
                .wait(350);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
