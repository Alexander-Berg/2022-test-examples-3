import { UserRole } from 'entities/User/consts/UserRole';
import { getUserRoles } from 'entities/User/helpers/getUserRoles/getUserRoles';

describe('getUserRoles', function () {
    it('works with empty params', function () {
        expect(getUserRoles([])).toMatchInlineSnapshot(`Array []`);
    });

    it('works with full params', function () {
        expect(getUserRoles([UserRole.ADMIN])).toMatchInlineSnapshot(`
            Array [
              "Admin",
            ]
        `);
        expect(getUserRoles([UserRole.DRIVER])).toMatchInlineSnapshot(`
            Array [
              "Driver",
            ]
        `);
        expect(getUserRoles([UserRole.ADMIN, UserRole.DRIVER])).toMatchInlineSnapshot(`
            Array [
              "Admin",
              "Driver",
            ]
        `);
    });

    it('works with unknown params', function () {
        expect(getUserRoles([UserRole.ADMIN, 'lol' as UserRole])).toMatchInlineSnapshot(`
            Array [
              "Admin",
              "—",
            ]
        `);
    });
});
