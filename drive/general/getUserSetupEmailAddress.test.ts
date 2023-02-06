import { getUserSetupEmailAddress } from 'entities/User/helpers/getUserSetupEmailAddress/getUserSetupEmailAddress';
import { UserSetupSchema } from 'entities/User/types/UserSetupSchema';

describe('getUserSetupEmailAddress', function () {
    it('works with filled params', function () {
        expect(getUserSetupEmailAddress({ email: { address: '123' } } as UserSetupSchema)).toStrictEqual('123');
    });
});
