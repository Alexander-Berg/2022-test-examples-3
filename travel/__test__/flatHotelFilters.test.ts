import {
    EBasicFilterGroupType,
    EDetailedFiltersBatchItemType,
    IBasicFilterGroup,
    IDetailedFiltersBatch,
} from 'types/hotels/search/IFiltersInfo';

import {flatHotelFilters} from 'projects/hotels/utilities/filters/flatHotelFilters';

const stars: IBasicFilterGroup = {
    id: '24',
    name: 'Звездность',
    type: EBasicFilterGroupType.MULTI,
    items: [
        {
            id: '1',
            name: '2 звезды',
            enabled: true,
            hint: '10',
            atoms: ['2:star'],
        },
        {
            id: '2',
            name: '3 звезды',
            enabled: true,
            hint: '20',
            atoms: ['3:star'],
        },
        {
            id: '3',
            name: '4 звезды',
            enabled: true,
            hint: '50',
            atoms: ['4:star'],
        },
    ],
};

const ratings: IBasicFilterGroup = {
    id: '13213',
    name: 'Рейтинг',
    type: EBasicFilterGroupType.MULTI,
    items: [
        {
            id: '23',
            name: '1 и выше',
            enabled: true,
            hint: '10',
            atoms: ['1:rating'],
        },
        {
            id: '1323',
            name: '2 и выше',
            enabled: true,
            hint: '20',
            atoms: ['2:rating'],
        },
        {
            id: '123123',
            name: '3 и выше',
            enabled: true,
            hint: '50',
            atoms: ['3:rating'],
        },
        {
            id: '23243',
            name: '4 и выше',
            enabled: true,
            hint: '60',
            atoms: ['4:rating'],
        },
    ],
};

const batchedFilters: IDetailedFiltersBatch[] = [
    {
        items: [
            {
                type: EDetailedFiltersBatchItemType.GROUP,
                detailedFilters: stars,
            },
            {
                type: EDetailedFiltersBatchItemType.PRICE,
            },
        ],
    },
    {
        items: [
            {
                type: EDetailedFiltersBatchItemType.GROUP,
                detailedFilters: ratings,
            },
        ],
    },
];

describe('flatHotelFilters', () => {
    it('Вернёт плоский список фильтров', () => {
        expect(flatHotelFilters(batchedFilters)).toEqual([stars, ratings]);
    });
});
