const preparePageHeader = require('../../core/adapter').preparePageHeader;

describe('Cover header', () => {
    describe('в старом формате без header-type в данных.', () => {
        it('Без картинки должен быть тип host', () => {
            const header = preparePageHeader('Hostname');

            expect(header.type, 'Неправильный тип заголовка').toEqual('host');
            expect(header.content.content_type, 'Неправильный контент заголовка').toEqual('header-title');
        });

        it('Без картинки должен быть тип host при header_title в данных', () => {
            const header = preparePageHeader('Hostname', '', 'Header title');

            expect(header.type, 'Неправильный тип заголовка').toEqual('host');
            expect(header.content.content_type, 'Неправильный контент заголовка').toEqual('header-title');
            expect(header.content.content, 'Неправильный текст заголовка').toEqual('Header title');
        });

        it('С картинкой должен быть тип wide-logo', () => {
            const header = preparePageHeader('Hostname', 'imageurl');

            expect(header.type, 'Неправильный тип заголовка').toEqual('wide-logo');
            expect(header.content.content_type, 'Неправильный контент заголовка').toEqual('image');
        });
    });

    describe('в формате c header-type в данных.', () => {
        it('Для типа host должен быть только title', () => {
            const header = preparePageHeader('Hostname', 'imageurl', 'Header title', 'host');

            expect(header.type, 'Неправильный тип заголовка').toEqual('host');
            expect(header.content.content_type, 'Неправильный контент заголовка').toEqual('header-title');
            expect(header.content.content, 'Неправильный текст заголовка').toEqual('Header title');
        });

        it('Для типа logo должна быть только картинка', () => {
            const header = preparePageHeader('Hostname', 'imageurl', 'Header title', 'logo');

            expect(header.type, 'Неправильный тип заголовка').toEqual('wide-logo');
            expect(header.content.content_type, 'Неправильный контент заголовка').toEqual('image');
        });

        it('Для типа logo-host должна быть картинка и текст', () => {
            const header = preparePageHeader('Hostname', 'imageurl', 'Header title', 'logo-host');

            expect(header.type, 'Неправильный тип заголовка').toEqual('logo-host');
            expect(header.content[0].content_type, 'Неправильный контент заголовка: нет картинки').toEqual('image');
            expect(header.content[1].content_type, 'Неправильный контент заголовка').toEqual('header-title');
            expect(header.content[1].content, 'Неправильный текст заголовка').toEqual('Header title');
        });
    });
});
