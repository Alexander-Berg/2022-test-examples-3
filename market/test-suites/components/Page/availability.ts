'use strict';

import _ from 'lodash';
import {importSuite} from 'ginny';

import getRoutePageData from 'spec/lib/helpers/getRouteData';
import {isAllowed, getAllowedPermissions} from 'shared/permissions';

// @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
export default (user, routeName, routeParams, newOnly = false) => {
    const routePageData = getRoutePageData(routeName);
    const permissionsByVendor = getAllowedPermissions(user.permissions, routeParams.vendor);

    const userHasPermission = isAllowed(permissionsByVendor, routePageData.permissionsOnly);
    const isAvailable = newOnly ? _.isEmpty(permissionsByVendor) : userHasPermission;

    const params = {
        user,
        route: {
            routeName,
            routeParams,
        },
        userName: user.description,
        page: routePageData.menuCaption || routeName,
    };

    if (isAvailable) {
        return importSuite('Page/__isAvailable', {params});
    }

    return importSuite('Page/__isUnavailable', {params});
};
