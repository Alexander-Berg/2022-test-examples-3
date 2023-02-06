'use strict';

import notifications from 'spec/lib/page-mocks/notifications.json';
import notificationsEmpty from 'spec/lib/page-mocks/notifications-empty.json';

import makeListTest from './makeListTest';

// @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
export default ({user, url}) => ({
    suiteName: 'Dropdown',
    childSuites: [
        makeListTest({
            url,
            user,
            suiteName: 'Full List',
            data: notifications,
        }),
        makeListTest({
            url,
            user,
            suiteName: 'Empty List',
            data: notificationsEmpty,
        }),
    ],
});
