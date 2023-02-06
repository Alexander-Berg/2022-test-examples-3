import { fast_tags2 } from '../../components/InterfaceAdminConfig/adminConfigKeys';
import { GVARS_INTERFACE_ADMIN_PREFIX } from '../../constants';
import { getRawSetting2 } from '../../utils/getRawSetting';
import {
    adminUserReducer,
    EMPTY_ROLES,
    initPermissions,
    initRoles,
    initRules,
    initState,
    roleIsLoading,
} from '../adminUserReducer';
import {
    activeRole,
    fastTags2,
    passiveRole,
    permissionsData,
    requestRolesData,
    requestSettingsData,
    userId,
} from './adminUSerReducer.mock';

const blockRulesExpected = {
    AvailableSearchPanel: true,
    CallCenterOperator: false,
    Checking: true,
};

describe('adminUserReducer', () => {
    it('settings save as raw data (rawSettings)', () => {
        const store = {
            AdminUser: adminUserReducer(initState, initRules(requestSettingsData, null)),
        };
        const received = getRawSetting2(store, `${GVARS_INTERFACE_ADMIN_PREFIX}.${fast_tags2}`);
        const expected = fastTags2;
        expect(received).toEqual(expected);
    });

    it('roles: rolesRaw', () => {
        const store = {
            AdminUser: adminUserReducer(initState, initRoles(requestRolesData, null)),
        };
        const received = store.AdminUser.rolesRaw;
        const expected = requestRolesData;
        expect(received).toEqual(expected);
    });

    it('roles: activeRoles', () => {
        const store = {
            AdminUser: adminUserReducer(initState, initRoles(requestRolesData, null)),
        };
        const received = [
            store.AdminUser.activeRoles.hasOwnProperty(activeRole),
            store.AdminUser.activeRoles.hasOwnProperty(passiveRole),
        ];
        const expected = [true, false];
        expect(received).toEqual(expected);
    });

    it('init permissions', () => {
        const store = {
            AdminUser: adminUserReducer(initState, initPermissions(permissionsData, null)),
        };
        const received = store.AdminUser.permissions;
        const expected = permissionsData;
        expect(received).toEqual(expected);
    });

    it('rules', () => {
        const store = {
            AdminUser: adminUserReducer(initState, initRoles(requestRolesData, null)),
        };
        store.AdminUser = adminUserReducer(store.AdminUser, initRules(requestSettingsData, null));
        const received = store.AdminUser.rules;
        const expected = { 'major/*': true, 'major2/*': false };
        expect(received).toEqual(expected);
    });

    it('blockRules', () => {
        const store = {
            AdminUser: adminUserReducer(initState, initRoles(requestRolesData, null)),
        };
        store.AdminUser = adminUserReducer(store.AdminUser, initRules(requestSettingsData, null));

        const received = store.AdminUser.blockRules;
        const expected = blockRulesExpected;
        expect(received).toEqual(expected);
    });

    it('isLoading', () => {
        const store = {
            AdminUser: adminUserReducer(initState, roleIsLoading(true, null)),
        };

        const received = [store.AdminUser.roleIsLoading];

        store.AdminUser = adminUserReducer(store.AdminUser, roleIsLoading(false, null));
        received.push(store.AdminUser.roleIsLoading);
        store.AdminUser = adminUserReducer(store.AdminUser, roleIsLoading(true, null));
        received.push(store.AdminUser.roleIsLoading);

        const expected = [true, false, true];
        expect(received).toEqual(expected);
    });

    it('error: empty roles', () => {
        const store = {
            AdminUser: adminUserReducer(initState, initRoles([], null)),
        };

        const received = store.AdminUser.error;

        const expected = EMPTY_ROLES;
        expect(received).toEqual(expected);
    });

    it('error: custom', () => {
        const error = { error: 1 };
        const store = {
            AdminUser: adminUserReducer(initState, initRoles(requestRolesData, error)),
        };

        const received = store.AdminUser.error;

        const expected = error;
        expect(received).toEqual(expected);
    });

    it('userId', () => {
        const store = {
            AdminUser: adminUserReducer(initState, initRoles(requestRolesData, null)),
        };

        const received = store.AdminUser.userId;

        const expected = userId;
        expect(received).toEqual(expected);
    });
});
