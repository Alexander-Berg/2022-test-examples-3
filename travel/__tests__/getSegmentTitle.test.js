const getSegmentTitle = require.requireActual('../getSegmentTitle').default;
const CHAR_EM_DASH = require.requireActual('../../stringUtils').CHAR_EM_DASH;

describe('getSegmentTitle', () => {
    it('Если передан заголовок, возвращаем его', () => {
        expect(getSegmentTitle({title: 'Москва - Петушки'})).toEqual(
            'Москва - Петушки',
        );
    });

    it('Если нет заголовка и переданы stationTo и stationFrom, используем их чтобы собрать строку', () => {
        expect(
            getSegmentTitle({
                stationFrom: {title: 'Екатеринбург'},
                stationTo: {title: 'Москва'},
            }),
        ).toEqual(`Екатеринбург ${CHAR_EM_DASH} Москва`);

        expect(
            getSegmentTitle({
                title: '',
                stationFrom: {title: 'Екатеринбург'},
                stationTo: {title: 'Москва'},
            }),
        ).toEqual(`Екатеринбург ${CHAR_EM_DASH} Москва`);
    });

    it('Если нет заголовка и не передан stationTo или stationFrom или заголовка одного из них, возвращаем пустую строку', () => {
        expect(getSegmentTitle({stationFrom: {title: 'Екатеринбург'}})).toEqual(
            '',
        );

        expect(getSegmentTitle({stationTo: {title: 'Москва'}})).toEqual('');

        expect(
            getSegmentTitle({stationFrom: {}, stationTo: {title: 'Москва'}}),
        ).toEqual('');

        expect(
            getSegmentTitle({
                stationFrom: {title: 'Екатеринбург'},
                stationTo: {},
            }),
        ).toEqual('');

        expect(getSegmentTitle({stationFrom: {}, stationTo: {}})).toEqual('');

        expect(getSegmentTitle({})).toEqual('');
    });
});
