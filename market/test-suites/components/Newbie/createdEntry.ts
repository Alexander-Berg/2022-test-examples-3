'use strict';

import initialState from 'spec/lib/page-mocks/entry-new.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import EntryForm from 'spec/page-objects/EntryForm';
import {User} from 'spec/lib/constants/users/users';

type Options = {user: User; url: string};

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Entry',
        selector: EntryForm.root,
        state: {
            vendorsEntries: initialState,
        },
        before(actions) {
            actions.waitForElementToShow(EntryForm.root, 10000);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
