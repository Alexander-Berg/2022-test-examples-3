import { needOpenChatInNewTab } from '../tab';

describe('#needOpenChatInNewTab', () => {
    it('должен вернуть true для внешнего сервиса', () => {
        expect(needOpenChatInNewTab(true, 'example.com')).toBeTruthy();
    });

    it('должен вернуть false для yandex.ru', () => {
        expect(needOpenChatInNewTab(true, 'yandex.ru')).toBeFalsy();
    });

    it('должен вернуть false для example.yandex-team.ru', () => {
        expect(needOpenChatInNewTab(true, 'example.yandex-team.ru')).toBeFalsy();
    });

    it('должен вернуть false для не Сафари', () => {
        expect(needOpenChatInNewTab(false, 'example.com')).toBeFalsy();
        expect(needOpenChatInNewTab(false, 'yandex.ru')).toBeFalsy();
    });
});
