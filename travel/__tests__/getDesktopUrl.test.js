jest.disableAutomock();

import getDesktopUrl from '../getDesktopUrl';

const desktopUrl = 'https://rasp.yandex.ru';
const touchUrl = 'https://t.rasp.yandex.ru';

const localDesktopUrl = 'https://l.rasp.yandex.ru:3000';
const localTouchUrl = 'https://l.t.rasp.yandex.ru:3000';

const experimentsDesktopUrl =
    'https://experiments.morda-front.rasp.common.yandex.ru/';
const experiments2DesktopUrl =
    'https://experiments2.morda-front.rasp.common.yandex.ru/';
const experimtntsTouchUrl =
    'https://t.experiments.morda-front.rasp.common.yandex.ru/';
const experimtnts2TouchUrl =
    'https://t.experiments2.morda-front.rasp.common.yandex.ru/';

const testingDesktopUrl = 'https://testing.morda-front.rasp.common.yandex.ru';
const oldTestingTouchUrl = 'https://testing.touch.rasp.common.yandex.ru';

const testingTouchUrl = 'https://t.testing.morda-front.rasp.common.yandex.ru';

describe('getDesktopUrl', () => {
    it('Вернёт пустую ссылку', () => {
        expect(getDesktopUrl('')).toBe('');
    });

    it('Вернёт ссылку без изменений', () => {
        expect(getDesktopUrl(desktopUrl)).toBe(desktopUrl);
    });

    it('Вернёт ссылку без изменений при локальной разработке', () => {
        expect(getDesktopUrl(localDesktopUrl)).toBe(localDesktopUrl);
    });

    it('Вернёт ссылку на десктоп', () => {
        expect(getDesktopUrl(touchUrl)).toBe(desktopUrl);
    });

    it('Вернёт ссылку на десктоп при локальной разработке', () => {
        expect(getDesktopUrl(localTouchUrl)).toBe(localDesktopUrl);
    });

    it('Вернёт ссылку без изменений для эксперимента', () => {
        expect(getDesktopUrl(experimentsDesktopUrl)).toBe(
            experimentsDesktopUrl,
        );
    });

    it('Вернёт ссылку без изменений для эксперимента 2', () => {
        expect(getDesktopUrl(experiments2DesktopUrl)).toBe(
            experiments2DesktopUrl,
        );
    });

    it('Вернёт ссылку на десктоп для эксперимента', () => {
        expect(getDesktopUrl(experimtntsTouchUrl)).toBe(experimentsDesktopUrl);
    });

    it('Вернёт ссылку на десктоп для эксперимента 2', () => {
        expect(getDesktopUrl(experimtnts2TouchUrl)).toBe(
            experiments2DesktopUrl,
        );
    });

    it('Вернёт ссылку без изменений для тестинга', () => {
        expect(getDesktopUrl(testingDesktopUrl)).toBe(testingDesktopUrl);
    });

    it('Вернёт ссылку на десктоп для тестинга', () => {
        expect(getDesktopUrl(testingTouchUrl)).toBe(testingDesktopUrl);
    });

    it('Заменит только первое вхождение', () => {
        expect(getDesktopUrl(`${touchUrl}?retpath${touchUrl}`)).toBe(
            `${desktopUrl}?retpath${touchUrl}`,
        );
    });

    it('Заменит ссылку для qloud окружений', () => {
        expect(getDesktopUrl(oldTestingTouchUrl)).toBe(testingDesktopUrl);
    });
});
