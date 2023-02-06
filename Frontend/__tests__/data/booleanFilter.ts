const filter = {
    id: '7013269',
    type: 'boolean',
    name: 'NFC',
    subType: '',
    meta: {},
};

export const booleanFilter = {
    ...filter,
    values: [
        { found: 23, value: '1', id: '1' },
        { found: 23, value: '0', id: '0' },
    ],
};

export const booleanFilterWithSelectedVal = {
    ...filter,
    values: [
        { found: 23, checked: true, value: '1', id: '1' },
        { found: 23, value: '0', id: '0' },
    ],
};
