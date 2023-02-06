import { getUserSetupPhoneNumber } from 'entities/User/helpers/getUserSetupPhoneNumber/getUserSetupPhoneNumber';
import { UserSetupSchema } from 'entities/User/types/UserSetupSchema';

describe('getUserSetupPhoneNumber', function () {
    it('works with filled params', function () {
        expect(getUserSetupPhoneNumber({ phone: { number: '123' } } as UserSetupSchema)).toStrictEqual('123');
    });
});
