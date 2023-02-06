'use strict';

import USERS from 'spec/lib/constants/users/users';
import buildUrl from 'spec/lib/helpers/buildUrl';
import permitCreator from 'spec/lib/helpers/permit';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import menu from 'spec/gemini/test-suites/components/Menu';

const permit = permitCreator(ROUTE_NAMES.QUESTIONS);

export default {
    suiteName: 'Questions',
    childSuites: USERS.reduce((suites, user) => {
        const vendor = 3301;
        const url = buildUrl(ROUTE_NAMES.QUESTIONS, {vendor});
        const {hasAccessToPage, permissionsByVendor} = permit(user, vendor);

        if (hasAccessToPage) {
            return suites.concat({
                // @ts-expect-error(TS2769) найдено в рамках VNDFRONT-4580
                suiteName: user.alias,
                childSuites: [menu({user, permissionsByVendor, vendor, url})],
            });
        }

        return suites;
    }, []),
};
