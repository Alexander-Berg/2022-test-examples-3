import { convertMapUrl } from '../helpers';

describe('Хелпер convertMapUrl', () => {
    it('должен преобразовывать ссылку в адрес виджета карты', () => {
        const url = 'https://yandex.ru/maps/54/yekaterinburg';

        expect(convertMapUrl(url)).toBe('https://yandex.ru/map-widget/v1/54/yekaterinburg');
    });

    it('должен преобразовывать ссылку на турецкие карты в адрес виджета карты', () => {
        const url = 'https://yandex.com.tr/harita/11508/istanbul/';

        expect(convertMapUrl(url)).toBe('https://yandex.com.tr/map-widget/v1/11508/istanbul/');
    });
});
