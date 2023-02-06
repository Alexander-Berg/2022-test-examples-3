import IStationStop from '../../../interfaces/state/station/IStationStop';

import getFunctionForGetDataStationSuggest from '../getFunctionForGetDataStationSuggest';

const stops: IStationStop[] = [
    {
        id: 1,
        title: 'Главная',
        majority: 1,
        threads: [],
        settlement: 'Качканар',
    },
    {
        id: 1,
        title: 'ВИЗ',
        majority: 2,
        threads: [],
        settlement: 'Екатеринбург',
    },
    {
        id: 1,
        title: 'Черёмушки',
        majority: 2,
        threads: [],
        settlement: 'Екатеринбург',
    },
    {
        id: 1,
        title: 'Патруши',
        majority: 2,
        threads: [],
        settlement: 'Екатеринбург',
    },
    {
        id: 1,
        title: 'Живенькая',
        majority: 1,
        threads: [],
        settlement: 'Ореховский район',
    },
    {
        id: 1,
        title: 'Ореховская',
        majority: 4,
        threads: [],
        settlement: 'трасса',
    },
    {
        id: 1,
        title: 'Ореховка',
        majority: 3,
        threads: [],
        settlement: 'Помпасьён',
    },
];

const getData = getFunctionForGetDataStationSuggest(stops, 2);

describe('getFunctionForGetDataStationSuggest', () => {
    it('Регистонезависимый поиск', () => {
        expect(getData('ви')).toStrictEqual({
            elements: [
                {
                    id: 1,
                    title: 'ВИЗ',
                    subtitle: 'Екатеринбург',
                },
            ],
        });
    });

    it('Поиск так же ведется по settlement', () => {
        expect(getData('Екатеринб')).toStrictEqual({
            elements: [
                {
                    id: 1,
                    title: 'ВИЗ',
                    subtitle: 'Екатеринбург',
                },
                {
                    id: 1,
                    title: 'Патруши',
                    subtitle: 'Екатеринбург',
                },
            ],
        });
    });

    it('Лимит для результата', () => {
        const getDataWithLowLimit = getFunctionForGetDataStationSuggest(
            stops,
            1,
        );

        expect(getDataWithLowLimit('Екатеринб')).toStrictEqual({
            elements: [
                {
                    id: 1,
                    title: 'ВИЗ',
                    subtitle: 'Екатеринбург',
                },
            ],
        });

        const getDataWithBigLimit = getFunctionForGetDataStationSuggest(
            stops,
            100,
        );

        expect(getDataWithBigLimit('Екатеринб')).toStrictEqual({
            elements: [
                {
                    id: 1,
                    title: 'ВИЗ',
                    subtitle: 'Екатеринбург',
                },
                {
                    id: 1,
                    title: 'Патруши',
                    subtitle: 'Екатеринбург',
                },
                {
                    id: 1,
                    title: 'Черёмушки',
                    subtitle: 'Екатеринбург',
                },
            ],
        });
    });

    it('При поиске буквы "е" и "ё" воспринимаются как "е"', () => {
        expect(getData('черем')).toStrictEqual({
            elements: [
                {
                    id: 1,
                    title: 'Черёмушки',
                    subtitle: 'Екатеринбург',
                },
            ],
        });

        expect(getData('ёкатеринбург')).toStrictEqual({
            elements: [
                {
                    id: 1,
                    title: 'ВИЗ',
                    subtitle: 'Екатеринбург',
                },
                {
                    id: 1,
                    title: 'Патруши',
                    subtitle: 'Екатеринбург',
                },
            ],
        });
    });

    it(
        'Сортировка результатов: если произошло совпадение по названию станции, то ' +
            'оно должно быть выше совпадения по названию населенного пункта',
        () => {
            const getDataFunc = getFunctionForGetDataStationSuggest(stops, 3);

            expect(getDataFunc('орехов')).toStrictEqual({
                elements: [
                    {
                        id: 1,
                        title: 'Ореховка',
                        subtitle: 'Помпасьён',
                    },
                    {
                        id: 1,
                        title: 'Ореховская',
                        subtitle: 'трасса',
                    },
                    {
                        id: 1,
                        title: 'Живенькая',
                        subtitle: 'Ореховский район',
                    },
                ],
            });
        },
    );
});
