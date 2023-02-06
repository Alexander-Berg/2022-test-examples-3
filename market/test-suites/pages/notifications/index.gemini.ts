'use strict';

import _ from 'lodash';

import USERS from 'spec/lib/constants/users/users';
import buildUrl from 'spec/lib/helpers/buildUrl';
import permitCreator from 'spec/lib/helpers/permit';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import P from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import menu from 'spec/gemini/test-suites/components/Menu';
import bell from 'spec/gemini/test-suites/components/Bell';

const permit = permitCreator(ROUTE_NAMES.NOTIFICATIONS);

export default {
    suiteName: 'Notifications',
    childSuites: USERS.reduce((suites, user) => {
        const vendor = _.get(user.permissions, [P.notifications.read, 0], 3301);
        const url = buildUrl(ROUTE_NAMES.NOTIFICATIONS, {vendor});
        const {hasAccessToPage, permissionsByVendor} = permit(user, vendor);

        if (hasAccessToPage) {
            return suites.concat({
                // @ts-expect-error(TS2769) найдено в рамках VNDFRONT-4580
                suiteName: user.alias,
                childSuites: [menu({user, vendor, permissionsByVendor, url}), bell({user, url})],
            });
        }

        return suites;
    }, []),
};
