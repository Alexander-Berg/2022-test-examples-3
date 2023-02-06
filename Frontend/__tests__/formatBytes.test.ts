import i18n from '../../../shared/lib/i18n';

import * as en from '../../../langs/yamb/en.json';
import * as ru from '../../../langs/yamb/ru.json';

import { formatBytes } from '../formatBytes';

describe('formatBytes', () => {
    describe('#getFileSizeStateless en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('Should return size in kb', () => {
            expect(formatBytes(20000)).toEqual('19.5 KB');
        });

        it('Should return size in yb', () => {
            expect(formatBytes(20 * Math.pow(1024, 8))).toEqual('20.0 YB');
        });

        it('Should return more yb size in yb', () => {
            expect(formatBytes(20 * Math.pow(1024, 9))).toEqual('20480.0 YB');
        });

        it('Should fallback to zero', () => {
            // Типизация formatBytes не поддерживает передачу undefined
            expect(formatBytes(undefined as unknown as number)).toEqual('0 B');
        });
    });

    describe('#getFileSizeStateless ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('Should return size in kb', () => {
            expect(formatBytes(20000)).toEqual('19,5 КБ');
        });

        it('Should return size in yb', () => {
            expect(formatBytes(20 * Math.pow(1024, 8))).toEqual('20,0 ЙБ');
        });

        it('Should return more yb size in yb', () => {
            expect(formatBytes(20 * Math.pow(1024, 9))).toEqual('20480,0 ЙБ');
        });

        it('Should fallback to zero', () => {
            // Типизация formatBytes не поддерживает передачу undefined
            expect(formatBytes(undefined as unknown as number)).toEqual('0 Б');
        });
    });
});
