'use strict';

import initialState from 'spec/lib/page-mocks/entry-cancelled.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import Newbie from 'spec/page-objects/Newbie';
import {User} from 'spec/lib/constants/users/users';

type Options = {user: User; url: string};

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Form',
        selector: Newbie.root,
        state: {
            vendorsEntries: initialState,
        },
        before(actions) {
            actions.waitForElementToShow(Newbie.root, 10000);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
