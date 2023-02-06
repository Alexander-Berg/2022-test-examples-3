import { isAliasForUserAddress } from '../../../../components/redux/store/selectors/user';

const getState = ({
    sids = [],
    emails = []
}) => ({
    user: { sids, emails }
});

describe('user selectors', () => {
    describe('isAliasForUserAddress', () => {
        it('should return true if an alias is in a user emails list', () => {
            expect(isAliasForUserAddress(getState({
                emails: ['User+tag@example.org']
            }), 'User+tag@Example.Org')).toBe(true);
        });

        it('should return false if an alias is not in a user`s emails list', () => {
            expect(isAliasForUserAddress(getState({}))).toBe(false);
        });
    });
});
