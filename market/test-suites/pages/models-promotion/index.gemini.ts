'use strict';

import _ from 'lodash';

import USERS from 'spec/lib/constants/users/users';
import buildUrl from 'spec/lib/helpers/buildUrl';
import permitCreator from 'spec/lib/helpers/permit';
import {patchSuite} from 'spec/gemini/lib/gemini-utils';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import P from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import menu from 'spec/gemini/test-suites/components/Menu';
import models from 'spec/gemini/test-suites/components/Models';
import bidsHint from 'spec/gemini/test-suites/components/Models/bidsHint';
import bidFrom from 'spec/gemini/test-suites/components/Models/bidFrom';
import bidTo from 'spec/gemini/test-suites/components/Models/bidTo';
import priceFrom from 'spec/gemini/test-suites/components/Models/priceFrom';
import priceTo from 'spec/gemini/test-suites/components/Models/priceTo';
import {MakeSuiteProps} from 'spec/gemini/lib/types';

const permit = permitCreator(ROUTE_NAMES.MODELS_PROMOTION);

export default {
    suiteName: 'Models promotion',
    childSuites: USERS.reduce((suites, user) => {
        const vendor = _.get(user.permissions, [P.modelbids.read, 0], 3301);
        const url = buildUrl(ROUTE_NAMES.MODELS_PROMOTION, {vendor});
        const {hasAccessToPage, permissionsByVendor, has} = permit(user, vendor);

        if (hasAccessToPage) {
            const childSuites = [
                menu({user, permissionsByVendor, vendor, url}),
                models({user, url}),
                patchSuite(bidFrom, {user, url}),
                patchSuite(bidTo, {user, url}),
                patchSuite(priceFrom, {user, url}),
                patchSuite(priceTo, {user, url}),
            ];

            if (has(P.modelbids.write)) {
                childSuites.push(bidsHint({user, url}));
            }

            return suites.concat({
                childSuites,
                suiteName: user.alias,
            });
        }

        return suites;
    }, [] as MakeSuiteProps['childSuites']),
} as MakeSuiteProps;
