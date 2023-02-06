import urlsPatchersHelper from '../../../components/helpers/urls-patchers';

describe('urlsPatchersHelper', () => {
    describe('Метод `storage`', () => {
        it('не должен упасть, если передать неправильный аргумент', () => {
            expect(() => {
                urlsPatchersHelper.storage({});
            }).not.toThrow();
            expect(() => {
                urlsPatchersHelper.storage();
            }).not.toThrow();
        });
        it('не должен поменять аргумент', () => {
            const input = { a: 5 };
            const expectation = Object.assign({}, input);

            urlsPatchersHelper.storage(input);
            expect(input).toEqual(expectation);
        });
        it('должен только срезать протокол, не заменив `tld`', () => {
            const http_file_input = { resource: { file: 'http://example.net' } };
            const http_file_expectation = { resource: { file: '//example.net' } };
            const https_file_input = { resource: { file: 'https://example.net' } };
            const https_file_expectation = { resource: { file: '//example.net' } };
            const http_folder_input = { resource: { folder: 'http://example.net' } };
            const http_folder_expectation = { resource: { folder: '//example.net' } };
            const https_folder_input = { resource: { folder: 'https://example.net' } };
            const https_folder_expectation = { resource: { folder: '//example.net' } };
            const http_file_no_tld_domains_input = { resource: { file: 'http://example.com' } };
            const http_file_no_tld_domains_expectation = { resource: { file: '//example.com' } };
            const https_file_no_tld_domains_input = { resource: { file: 'https://example.com' } };
            const https_file_no_tld_domains_expectation = { resource: { file: '//example.com' } };
            const http_folder_no_tld_domains_input = { resource: { folder: 'http://example.com' } };
            const http_folder_no_tld_domains_expectation = { resource: { folder: '//example.com' } };
            const https_folder_no_tld_domains_input = { resource: { folder: 'https://example.com' } };
            const https_folder_no_tld_domains_expectation = { resource: { folder: '//example.com' } };

            urlsPatchersHelper.storage(http_file_input);
            expect(http_file_input).toEqual(http_file_expectation);

            urlsPatchersHelper.storage(https_file_input);
            expect(https_file_input).toEqual(https_file_expectation);

            urlsPatchersHelper.storage(http_folder_input);
            expect(http_folder_input).toEqual(http_folder_expectation);

            urlsPatchersHelper.storage(https_folder_input);
            expect(https_folder_input).toEqual(https_folder_expectation);

            urlsPatchersHelper.storage(http_file_no_tld_domains_input);
            expect(http_file_no_tld_domains_input).toEqual(http_file_no_tld_domains_expectation);

            urlsPatchersHelper.storage(https_file_no_tld_domains_input);
            expect(https_file_no_tld_domains_input).toEqual(https_file_no_tld_domains_expectation);

            urlsPatchersHelper.storage(http_folder_no_tld_domains_input);
            expect(http_folder_no_tld_domains_input).toEqual(http_folder_no_tld_domains_expectation);

            urlsPatchersHelper.storage(https_folder_no_tld_domains_input);
            expect(https_folder_no_tld_domains_input).toEqual(https_folder_no_tld_domains_expectation);
        });
        it('должен срезать протокол и заменить `tld`', () => {
            const http_file_input = { resource: { file: 'http://example.com' }, tld: 'ua', domains: { ua: { domain: 'ua' } } };
            const http_file_expectation = { resource: { file: '//example.ua' }, tld: 'ua', domains: { ua: { domain: 'ua' } } };
            const https_file_input = { resource: { file: 'https://example.com' }, tld: 'ua', domains: { ua: { domain: 'ua' } } };
            const https_file_expectation = { resource: { file: '//example.ua' }, tld: 'ua', domains: { ua: { domain: 'ua' } } };
            const http_folder_input = { resource: { folder: 'http://example.com' }, tld: 'ua', domains: { ua: { domain: 'ua' } } };
            const http_folder_expectation = { resource: { folder: '//example.ua' }, tld: 'ua', domains: { ua: { domain: 'ua' } } };
            const https_folder_input = { resource: { folder: 'https://example.com' }, tld: 'ua', domains: { ua: { domain: 'ua' } } };
            const https_folder_expectation = { resource: { folder: '//example.ua' }, tld: 'ua', domains: { ua: { domain: 'ua' } } };

            urlsPatchersHelper.storage(http_file_input);
            expect(http_file_input).toEqual(http_file_expectation);

            urlsPatchersHelper.storage(https_file_input);
            expect(https_file_input).toEqual(https_file_expectation);

            urlsPatchersHelper.storage(http_folder_input);
            expect(http_folder_input).toEqual(http_folder_expectation);

            urlsPatchersHelper.storage(https_folder_input);
            expect(https_folder_input).toEqual(https_folder_expectation);
        });
    });
});
