import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import makeUrl from '../makeUrl';
import getNationalLanguage from '../../lang/getNationalLanguage';

jest.mock('../../lang/getNationalLanguage');
(getNationalLanguage as jest.Mock).mockImplementation(() => Lang.ru);

describe('makeUrl', () => {
    it('Get-параметры сортируются, делая урл устойчивым. Пустые параметры удаляются.', () => {
        const query = {
            fromId: 'c2',
            toId: 'c213',
            fromName: 'Saint Petersburg',
            toName: 'Москва',
            transport: ['train', 'air plane', null],
            stationFrom: [null, undefined],
            highSpeedTrain: [],
            aeroex: null,
        };

        const url = makeUrl('/search/', Tld.ru, Lang.ru, query);

        expect(url).toBe(
            '/search/?fromId=c2&fromName=Saint+Petersburg&toId=c213&toName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&transport%5B0%5D=train&transport%5B1%5D=air+plane',
        );
    });

    it('В случае, когда выбранный язык не является языком по умолчанию для данного домена - он будет добавлен к ссылке', () => {
        (getNationalLanguage as jest.Mock).mockReturnValueOnce(Lang.uk);

        expect(makeUrl('/search', Tld.ru, Lang.ru, {test: '1'})).toBe(
            '/search?lang=ru&test=1',
        );
    });

    it('Вместо пробела используется "+"', () => {
        expect(makeUrl('/test', Tld.ru, Lang.ru, {test: 'space space'})).toBe(
            '/test?test=space+space',
        );
    });

    it('Не добавит "?", если нет get-параметров', () => {
        expect(makeUrl('/test', Tld.ru, Lang.ru)).toBe('/test');
    });
});
