import { getAvatarImage } from '../../../components/helpers/b2b';

const base64Prefix = 'data:image/svg+xml;base64,';

describe('helperB2b', () => {
    describe('#getAvatarImage ->', () => {
        it('аватар по умолчанию', () => {
            let avatarImage = getAvatarImage({});
            expect(avatarImage.startsWith(base64Prefix)).toBe(true);
            avatarImage = avatarImage.replace(base64Prefix, '');
            expect(atob(avatarImage)).toBe(
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><g><rect height="100%" width="100%" fill="#9a9a9a"/><g fill="#fff"><path d="M42.1 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6s-6.3 0-6.6 3.6c-.6 4.5 2.1 13.8 3 14.7 2.4 1.5 1.5 3.9.9 5.1 0 .3-.3.6-.3.6-.6 1.5-3.9 3.9-10.2 7.2l-1.5-2.7c3.9-2.1 8.1-4.8 8.7-5.7 0-.3.3-.6.3-.9.3-.3.6-1.2.3-1.2-2.7-1.8-5.1-12.6-4.5-17.7.6-6.3 9.3-6.3 9.6-6.3.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-.9 2.4z"/><path d="M52.6 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6v-3c.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-1.2 2.1z"/><path d="M62.8 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6v-3c.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-1.2 2.1z"/></g></g></svg>'
            );
        });

        it('аватар для группы', () => {
            let avatarImage = getAvatarImage({
                type: 'group',
                name: 'Яндекс Диск'
            });
            expect(avatarImage.startsWith(base64Prefix)).toBe(true);
            avatarImage = avatarImage.replace(base64Prefix, '');
            expect(atob(avatarImage)).toBe(
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><g><rect height="100%" width="100%" fill="#9a9a9a"/><g fill="#fff"><path d="M42.1 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6s-6.3 0-6.6 3.6c-.6 4.5 2.1 13.8 3 14.7 2.4 1.5 1.5 3.9.9 5.1 0 .3-.3.6-.3.6-.6 1.5-3.9 3.9-10.2 7.2l-1.5-2.7c3.9-2.1 8.1-4.8 8.7-5.7 0-.3.3-.6.3-.9.3-.3.6-1.2.3-1.2-2.7-1.8-5.1-12.6-4.5-17.7.6-6.3 9.3-6.3 9.6-6.3.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-.9 2.4z"/><path d="M52.6 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6v-3c.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-1.2 2.1z"/><path d="M62.8 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6v-3c.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-1.2 2.1z"/></g></g></svg>'
            );
        });

        it('аватар для подразделения', () => {
            let avatarImage = getAvatarImage({
                type: 'department',
                name: 'YandexDisk'
            });
            expect(avatarImage.startsWith(base64Prefix)).toBe(true);
            avatarImage = avatarImage.replace(base64Prefix, '');
            expect(atob(avatarImage)).toBe(
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><g><rect height="100%" width="100%" fill="#9a9a9a"/><g fill="#fff"><path d="M42.1 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6s-6.3 0-6.6 3.6c-.6 4.5 2.1 13.8 3 14.7 2.4 1.5 1.5 3.9.9 5.1 0 .3-.3.6-.3.6-.6 1.5-3.9 3.9-10.2 7.2l-1.5-2.7c3.9-2.1 8.1-4.8 8.7-5.7 0-.3.3-.6.3-.9.3-.3.6-1.2.3-1.2-2.7-1.8-5.1-12.6-4.5-17.7.6-6.3 9.3-6.3 9.6-6.3.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-.9 2.4z"/><path d="M52.6 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6v-3c.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-1.2 2.1z"/><path d="M62.8 56.8c-6.3-3.3-9.6-5.7-10.2-7.2 0-.3-.3-.6-.3-.6-.6-1.2-1.5-3.6.9-5.1 1.2-1.2 3.6-10.2 3-14.7-.3-3.6-6.6-3.6-6.6-3.6v-3c.3 0 9 0 9.6 6.3.6 5.1-1.8 16.2-4.5 17.7-.3.3 0 .9.3 1.5 0 .3.3.6.3.9.6.6 4.8 3.6 8.7 5.7l-1.2 2.1z"/></g></g></svg>'
            );
        });

        it('аватар для пользователя (латиница)', () => {
            let avatarImage = getAvatarImage({
                type: 'user',
                name: 'Visiliy Pupkin'
            });
            expect(avatarImage.startsWith(base64Prefix)).toBe(true);
            avatarImage = avatarImage.replace(base64Prefix, '');
            expect(atob(avatarImage)).toBe(
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><g><rect height="100%" width="100%" fill="#7c7dba"/><text y="52.32542" x="41.423134" style="text-rendering:optimizeLegibility;font-size:36px;font-family:sans-serif;text-anchor:middle;fill:#fff;">VP</text></g></svg>'
            );
        });

        it('аватар для пользователя (кириллица)', () => {
            let avatarImage = getAvatarImage({
                type: 'user',
                name: 'Василий Пупкин'
            });
            expect(avatarImage.startsWith(base64Prefix)).toBe(true);
            avatarImage = avatarImage.replace(base64Prefix, '');
            expect(atob(avatarImage)).toBe(
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><g><rect height="100%" width="100%" fill="#64b988"/><text y="52.32542" x="41.423134" style="text-rendering:optimizeLegibility;font-size:36px;font-family:sans-serif;text-anchor:middle;fill:#fff;">Ð\u0092Ð\u009f</text></g></svg>'
            );
        });
    });
});
