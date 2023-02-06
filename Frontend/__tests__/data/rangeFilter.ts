const filterBase = {
    id: '76890',
    type: 'number',
    name: 'Заголовок',
    subType: '',
    kind: 2,
    meta: {},
};

export const rangeFilter = {
    ...filterBase,
    values: [
        {
            max: '69900',
            min: '111',
            id: 'found',
        },
    ],
};

export const rangeFilterWithSelectedVal = {
    ...filterBase,
    values: [
        {
            max: '7878',
            min: '65',
            id: 'chosen',
        },
        {
            max: '69900',
            min: '111',
            id: 'found',
        },
    ],
};
