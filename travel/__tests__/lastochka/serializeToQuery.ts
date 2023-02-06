import lastochka from '../../lastochka';

describe('lastochka', () => {
    describe('serializeToQuery', () => {
        it('Показывает только ласточки', () => {
            const query = lastochka.serializeToQuery(true);

            expect(query).toEqual({lastochka: 'y'});
        });

        it('Показывает все сегменты', () => {
            const query = lastochka.serializeToQuery(false);

            expect(query).toEqual({});
        });
    });
});
