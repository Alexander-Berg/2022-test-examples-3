import { UserRole } from 'entities/User/consts/UserRole';
import { hasUserRoleDriver } from 'entities/User/helpers/hasUserRoleDriver/hasUserRoleDriver';

describe('hasUserRoleDriver', function () {
    it('works with empty params', function () {
        expect(hasUserRoleDriver([])).toBeFalsy();
    });

    it('works with full params', function () {
        expect(hasUserRoleDriver([UserRole.ADMIN])).toBeFalsy();
        expect(hasUserRoleDriver([UserRole.DRIVER])).toBeTruthy();
        expect(hasUserRoleDriver([UserRole.ADMIN, UserRole.DRIVER])).toBeTruthy();
    });
});
