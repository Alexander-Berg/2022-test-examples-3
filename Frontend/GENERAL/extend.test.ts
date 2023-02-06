import { extend } from './extend';

const defaultPageWithLcYandexForm = {
    block: 'lc-page',
    content: [
        {
            block: 'lc-yandex-form',
            customProvider: 'https://forms.yandex.ru/surveys/10029744.445a248a3887ab9ca79052d4b31d1a42cf1ad25c/?iframe=1&lang=ru&resume_type=file',
        }
    ],
};

describe('extend', () => {
    describe('default conversions', () => {
        it('should rename lc-yandex-form block to lc-jobs-yandex-form', () => {
            expect(
                extend(defaultPageWithLcYandexForm, []).content[0].block
            ).toEqual('lc-jobs-yandex-form');
        });
    });
});
