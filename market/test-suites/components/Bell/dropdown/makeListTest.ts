'use strict';

import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
import {makeElementInvisibleBySelector} from 'spec/gemini/helpers';
import Bell from 'spec/page-objects/Bell';
import Link from 'spec/page-objects/Link';
import LayoutBase from 'spec/page-objects/LayoutBase';

// @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580// @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
export default ({suiteName, data, url, user}) =>
    makeKadavrSuite({
        suiteName,
        url,
        user,
        state: {
            vendorsNotifications: data,
        },
        selector: Bell.dropdown,
        before(actions, find) {
            actions
                .click(find(`${Bell.root} > ${Link.root}`))
                // Ждём появления попапа
                .waitForElementToShow(Bell.dropdown, 10000)
                // Ждём загрузки списка
                .wait(7000);

            makeElementInvisibleBySelector.call(actions, LayoutBase.root);
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
