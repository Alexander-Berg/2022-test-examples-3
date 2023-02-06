const { getNewsLoadMore } = require('../../../../core/utils/limit-feed-config/load-more');

describe('Ограничение ленты - Показать ещё', () => {
    describe('Новости', () => {
        describe('Полностраничная лента', () => {
            it('Возвращает 1 если флаг со значением 1', () => {
                expect(getNewsLoadMore('full', { 'limit-feed-news-load-more-full': 1 })).toStrictEqual(1);
            });

            it('Возвращает 1 если флаг с другим значением', () => {
                expect(getNewsLoadMore('full', { 'limit-feed-news-load-more-full': 5 })).toStrictEqual(1);
            });

            it('Возвращает undefined если флаг не передан', () => {
                expect(getNewsLoadMore('full', {})).toBeUndefined();
            });
        });

        describe('Карточная лента', () => {
            it('Возвращает 1 если флаг со значением 1', () => {
                expect(getNewsLoadMore('cards', { 'limit-feed-news-load-more-cards': 1 })).toStrictEqual(1);
            });

            it('Возвращает 5 если флаг со значением 5', () => {
                expect(getNewsLoadMore('cards', { 'limit-feed-news-load-more-cards': 5 })).toStrictEqual(5);
            });

            it('Возвращает undefined если флаг не передан', () => {
                expect(getNewsLoadMore('cards', {})).toBeUndefined();
            });
        });

        it('Возвращает undefined если передан некорректный тип ленты', () => {
            expect(getNewsLoadMore('wrong', {})).toBeUndefined();
        });
    });
});
