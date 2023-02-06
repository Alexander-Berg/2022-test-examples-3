import { PreviewSizes, YapicAvatarSize } from '../../typings/previews';
import { getAvatarUrl } from '../avatar';

describe('Helpers files', () => {
    describe('#getAvatarUrl', () => {
        it('returns undefined when avatarId is undefined', () => {
            expect(getAvatarUrl()).toBe(undefined);
        });

        it('returns public small avatar by default', () => {
            expect(getAvatarUrl('my-test-avatar-id'))
                .toBe(`privateAvatarUrl?{\"avatarId\":\"my-test-avatar-id\",\"size\":\"${PreviewSizes.SMALL}\"}`);
        });

        it('returns public small avatar by default from public chat', () => {
            expect(getAvatarUrl('my-test-avatar-id', { isPublic: true }))
                .toBe(`publicAvatarUrl?{\"avatarId\":\"my-test-avatar-id\",\"size\":\"${PreviewSizes.SMALL}\"}`);
        });

        it('returns small48 avatar when size is small48', () => {
            expect(getAvatarUrl('my-test-avatar-id', { size: PreviewSizes.SMALL48 }))
                .toBe(`privateAvatarUrl?{\"avatarId\":\"my-test-avatar-id\",\"size\":\"${PreviewSizes.SMALL48}\"}`);
        });

        it('returns yapic avatar url when avatarId starts with user_avatar/yapic', () => {
            const testAvatarId = '123123';

            expect(getAvatarUrl(`user_avatar/yapic/${testAvatarId}`))
                .toBe(`yapicAvatarUrl?{\"avatarId\":\"123123\",\"size\":\"${YapicAvatarSize.SMALL}\"}`);
        });
    });
});
