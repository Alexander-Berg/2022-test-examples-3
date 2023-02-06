'use strict';

import initialState from 'spec/lib/page-mocks/subscribers.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import Notifications from 'spec/page-objects/Notifications';
import {User} from 'spec/lib/constants/users/users';

type Options = {user: User; url: string};

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Content',
        selector: Notifications.root,
        state: {
            vendorsSubscribers: initialState,
        },
        before(actions) {
            actions.waitForElementToShow(Notifications.root, 10000).wait(5000);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
