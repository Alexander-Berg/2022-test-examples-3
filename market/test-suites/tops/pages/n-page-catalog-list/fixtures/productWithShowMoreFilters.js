import {mergeState, createFilter, createFilterValue} from '@yandex-market/kadavr/mocks/Report/helpers';


/**
 * Создает значение группы фильтров
 * @param {Object} options - свойства группы
 * @param {String} filterId - id фильтра, группу для которого нужно создать
 * @param {String} type - тип группы
 * @returns {Object}
 */
const createFilterValuesGroup = (options, filterId, type) => {
    const valueId = `${filterId}_${type}`;

    return {
        collections: {
            filter: {
                [filterId]: {
                    valuesGroups: [valueId],
                },
            },
            filterValuesGroup: {
                [valueId]: {
                    ...options,
                    type,
                },
            },
        },
    };
};

const filterMock = {
    'id': '15161366',
    'type': 'enum',
    'name': 'Фильтр',
};

const filterValuesGroupsMock = [
    {
        'type': 'top',
        'valuesIds': [...Array(5).keys()],
    },
    {
        'type': 'all',
        'valuesIds': [...Array(10).keys()],
    },
];

const filter = createFilter(filterMock, filterMock.id);

const filterValues = [...Array(10).keys()]
    .map(i => createFilterValue({id: i, value: i}, filterMock.id, i));

const filterValuesGroups = filterValuesGroupsMock
    .map(filterValuesGroup => createFilterValuesGroup(filterValuesGroup, filterMock.id, filterValuesGroup.type));

const state = mergeState([
    filter,
    ...filterValues,
    ...filterValuesGroups,
]);

const route = {
    nid: '54726',
    slug: 'mobilnye-telefony',
};

export default {
    state,
    route,
    filterId: filterMock.id,
};
