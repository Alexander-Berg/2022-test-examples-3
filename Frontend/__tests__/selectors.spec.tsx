import { transformBlackbox } from '../selectors';

describe('Transform Blackbox', () => {
    it('должен вернуть null', () => {
        expect(transformBlackbox(undefined)).toEqual(null);
    });

    it('не падает, если не пришел display_name', () => {
        expect(transformBlackbox({
            dbfields: {
                'userinfo.firstname.uid': 'Test FirstName',
                'userinfo.lastname.uid': 'Test LastName',
            },
            login: 'test_login',
            display_name: undefined,
        })).toEqual({
            firstName: 'Test FirstName',
            lastName: 'Test LastName',
            addressList: undefined,
            login: 'test_login',
            displayName: undefined,
            phones: undefined,
            avatar: null,
        });
    });
});
