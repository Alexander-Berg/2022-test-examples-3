'use strict';

import USERS from 'spec/lib/constants/users/users';
import buildUrl from 'spec/lib/helpers/buildUrl';
import {patchSuite} from 'spec/gemini/lib/gemini-utils';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import {isManager, getAllowedPermissions} from 'shared/permissions';
import menu from 'spec/gemini/test-suites/components/Menu';
import entryForm from 'spec/gemini/test-suites/components/Entry';
import suggestBrands from 'spec/gemini/test-suites/components/Suggest/brands';
import multipassport from 'spec/gemini/test-suites/components/Multipassport';

export default {
    suiteName: 'Entries',
    childSuites: USERS.reduce((suites, user) => {
        const url = buildUrl(ROUTE_NAMES.ENTRIES);
        const permissions = getAllowedPermissions(user.permissions, 0);

        if (isManager(permissions)) {
            return suites.concat({
                // @ts-expect-error(TS2769) найдено в рамках VNDFRONT-4580
                suiteName: user.alias,
                childSuites: [
                    // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4580
                    menu({user, url}),
                    entryForm({user, url}),
                    patchSuite(suggestBrands, {user, url}),
                    patchSuite(multipassport, {user, url}),
                ],
            });
        }

        return suites;
    }, []),
};
