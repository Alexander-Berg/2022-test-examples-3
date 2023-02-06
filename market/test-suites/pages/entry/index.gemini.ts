'use strict';

import user from 'spec/lib/constants/users/profile/auto.entryCreatorUser';
import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import createdEntry from 'spec/gemini/test-suites/components/Newbie/createdEntry';
import entryForm from 'spec/gemini/test-suites/components/Newbie/entryForm';

const url = buildUrl(ROUTE_NAMES.ENTRY);

export default {
    suiteName: 'Entry',
    childSuites: [
        {
            suiteName: user.alias,
            childSuites: [createdEntry({url, user}), entryForm({url, user})],
        },
    ],
};
