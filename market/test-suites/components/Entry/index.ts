'use strict';

import initialState from 'spec/lib/page-mocks/entries.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import Entry from 'spec/page-objects/Entry';
import Form from 'spec/page-objects/Form';
import {User} from 'spec/lib/constants/users/users';

type Options = {
    user: User;
    url: string;
};

const entrySelector = `${Entry.root}:nth-child(1)`;

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Entry form',
        selector: entrySelector,
        state: {
            vendorsEntries: initialState,
        },
        before(actions, find) {
            actions
                .waitForElementToShow(entrySelector, 10000)
                .click(find(entrySelector))
                .waitForElementToShow(Form.root, 10000);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
