jest.disableAutomock();

import {getTouchAlternateLink} from '../../altLinks';

describe('getTouchAlternateLink', () => {
    it('Альтернативная ссылка на мобильную версию', () => {
        expect(getTouchAlternateLink('https://rasp.yandex.ru/train')).toEqual({
            media: 'only screen and (max-width: 640px)',
            rel: 'alternate',
            href: 'https://t.rasp.yandex.ru/train',
        });
    });
});
