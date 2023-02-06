import { Counters, IEvent, EventType } from '../Counters';

describe('Counters', () => {
    let event: IEvent;
    let baseUrl: string;
    let pageId: string;

    beforeEach(() => {
        event = { event: EventType.click, id: 'id_uniqId', name: 'counter' };
        baseUrl = '//www.yandex.ru/clck/safeclick/data=AiuY0DBWFJ5fN';
        pageId = 'page_id';

        Date.now = jest.fn(() => 1537787941792);

        Counters.setBaseUrl(undefined);
        Counters.setPageId(undefined);
    });

    it('Возвращается ошибка если нет baseUrl', () => {
        Counters.setPageId(pageId);
        const expectErr = /baseUrl/;

        expect(() => Counters.createUrl(event)).toThrowError(expectErr);
    });

    it('Возвращается ошибка если нет pageId', () => {
        Counters.setBaseUrl(baseUrl);
        const expectErr = /PageId/;

        expect(() => Counters.createUrl({ event: EventType.click, name: 'counter' })).toThrowError(expectErr);
    });

    it('Должен проставлять id страницы при отсутствии id элемента', () => {
        Counters.setBaseUrl(baseUrl);
        Counters.setPageId(pageId);

        const url = Counters.createUrl({ event: EventType.click, name: 'counter' });

        expect(url).toMatch(pageId);
    });

    it('Должен генерироваться корректный url', () => {
        Counters.setBaseUrl(baseUrl);

        const url = Counters.createUrl(event);

        let expected = baseUrl;
        expected += `/vars=-baobab-event-json=${encodeURIComponent(JSON.stringify([event]))}`;
        expected += `/cts=${Date.now()}`;
        expected += '/*http://localhost/';

        expect(url).toEqual(expected);
    });
});
