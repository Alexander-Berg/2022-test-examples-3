'use strict';

import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import {scrollToElementBySelector} from 'spec/gemini/helpers';
import ContactsForm from 'spec/page-objects/ContactsForm';
import {User} from 'spec/lib/constants/users/users';

type Options = {user: User; url: string};

export default ({user, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'ContactsForm',
        selector: ContactsForm.root,
        before(actions) {
            actions.waitForElementToShow(ContactsForm.root, 10000).wait(5000);
            scrollToElementBySelector.call(actions, ContactsForm.root);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
