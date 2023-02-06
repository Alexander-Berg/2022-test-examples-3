'use strict';

import USERS from 'spec/lib/constants/users/users';
import buildUrl from 'spec/lib/helpers/buildUrl';
import permitCreator from 'spec/lib/helpers/permit';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import menu from 'spec/gemini/test-suites/components/Menu';
import products from 'spec/gemini/test-suites/components/Products';

const permit = permitCreator(ROUTE_NAMES.PRODUCTS);

export default {
    suiteName: 'Products',
    childSuites: USERS.reduce((suites, user) => {
        // В проде у читающего менеджера нет доступа к вендору 3300
        // @ts-expect-error(TS2304) найдено в рамках VNDFRONT-4580
        const vendor = gemini.ctx.environment === 'production' ? 3301 : 3300;
        const url = buildUrl(ROUTE_NAMES.PRODUCTS, {vendor});
        const {hasAccessToPage, permissionsByVendor} = permit(user, vendor);

        if (hasAccessToPage) {
            return suites.concat({
                // @ts-expect-error(TS2769) найдено в рамках VNDFRONT-4580
                suiteName: user.alias,
                childSuites: [menu({user, permissionsByVendor, vendor, url}), products({user, url})],
            });
        }

        return suites;
    }, []),
};
