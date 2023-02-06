import { assert } from 'chai';

import type { ISnippetContext } from '@lib/Context/SnippetContext';
import { isSearchAppBookReaderAvailable } from './bookreader';

describe('searchApp', () => {
    describe('isSearchAppBookReaderAvailable', () => {
        it('should disable book reader in browser', function() {
            const context = { isSearchApp: false } as ISnippetContext;

            assert.isFalse(isSearchAppBookReaderAvailable(context));
        });

        it('should disable book reader in old SearchApp for IOS', function() {
            const context = {
                isSearchApp: true,
                device: {
                    BrowserName: 'YandexSearch',
                    OSFamily: 'iOS',
                    BrowserVersionRaw: '62.00',
                },
            } as ISnippetContext;

            assert.isFalse(isSearchAppBookReaderAvailable(context));
        });

        it('should enable book reader in modern SearchApp for IOS', function() {
            const context = {
                isSearchApp: true,
                device: {
                    BrowserName: 'YandexSearch',
                    OSFamily: 'iOS',
                    BrowserVersionRaw: '63.00',
                },
            } as ISnippetContext;

            assert.isTrue(isSearchAppBookReaderAvailable(context));
        });

        it('should disable book reader in old SearchApp for Android', function() {
            const context = {
                isSearchApp: true,
                device: {
                    BrowserName: 'YandexSearch',
                    OSFamily: 'Android',
                    BrowserVersionRaw: '21.21',
                },
            } as ISnippetContext;

            assert.isFalse(isSearchAppBookReaderAvailable(context));
        });

        it('should enable book reader in modern SearchApp for Android', function() {
            const context = {
                isSearchApp: true,
                device: {
                    BrowserName: 'YandexSearch',
                    OSFamily: 'Android',
                    BrowserVersionRaw: '21.31',
                },
            } as ISnippetContext;

            assert.isTrue(isSearchAppBookReaderAvailable(context));
        });
    });
});
