import { parseSource } from '../../utils/parseSource';

describe('parseSource()', () => {
    it('парсит сервис на кириллице через точку', () => {
        expect(parseSource('Яндекс.Маркет')).toStrictEqual('Яндекс');
    });

    it('парсит сервис на кириллице через пробел', () => {
        expect(parseSource('Яндекс Маркет')).toStrictEqual('Яндекс');
    });

    it('парсит сервис на кириллице без учёта регистра', () => {
        expect(parseSource('яндекс маркет')).toStrictEqual('Яндекс');
    });

    it('возвращает исходный сервис, если это не яндекс', () => {
        expect(parseSource('Ozon')).toStrictEqual('Ozon');
    });
});
