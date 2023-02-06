import { isPostMessageAllowedHost } from '../../../src/store/utils';

describe('store/utils', () => {
    describe('isPostMessageAllowedHost', () => {
        it('should return true for tutor', () => {
            expect(isPostMessageAllowedHost({
                origin: 'https://tutor.yandex.ru'
            }, {
                protocol: 'https:',
                host: 'sameorigin.host'
            })).toEqual(true);
        });

        it('should return true for sameorigin', () => {
            expect(isPostMessageAllowedHost({
                origin: 'https://sameorigin.host'
            }, {
                protocol: 'https:',
                host: 'sameorigin.host'
            })).toEqual(true);
        });

        it('should return false for not allowed host', () => {
            expect(isPostMessageAllowedHost({
                origin: 'https://not.allowed.host'
            }, {
                protocol: 'https:',
                host: 'sameorigin.host'
            })).toEqual(false);
        });
    });
});
