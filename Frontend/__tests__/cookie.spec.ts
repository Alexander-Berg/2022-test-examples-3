import { parseCookie } from '../cookie';

describe('cookie', () => {
    describe('parseCookie', () => {
        it('корректно достает и парсит куки из строки в хэшмап', () => {
            expect(parseCookie('js=1; fonts-loaded=1; ys=train=1=2; abc={test: "1=1"}; empty=')).toEqual({
                js: '1',
                'fonts-loaded': '1',
                ys: 'train=1=2',
                abc: '{test: "1=1"}',
            });
        });
    });
});
