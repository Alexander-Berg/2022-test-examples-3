'use strict';

import data from 'spec/lib/page-mocks/notifications.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import Bell from 'spec/page-objects/Bell';
import Link from 'spec/page-objects/Link';
import TextB2b from 'spec/page-objects/TextB2b';

const waitForCounterShow =
    // eslint-disable-next-line no-new-func
    new Function(`return window.document.querySelectorAll('${Bell.root} > ${Link.root} > ${TextB2b.root}').length > 0`);

// @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
export default ({user, url}) =>
    makeKadavrSuite({
        url,
        user,
        state: {
            vendorsNotifications: data,
        },
        suiteName: 'Counter',
        selector: `${Bell.root} > ${Link.root}`,
        before(actions) {
            actions.waitForJSCondition(waitForCounterShow, 10000);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
