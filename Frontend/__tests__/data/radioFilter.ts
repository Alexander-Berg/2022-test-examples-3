const filter = {
    id: '5065232',
    type: 'boolean',
    name: 'Шипы',
    subType: '',
    hasBoolNo: true,
    meta: {},
};

export const radioFilter = {
    ...filter,
    values: [
        { id: '1', value: '1', found: 22 },
        { id: '0', value: '0', found: 55 },
    ],
};

export const radioFilterWithSelectedVal = {
    ...filter,
    values: [
        { id: '1', value: '1', found: 22 },
        { id: '0', value: '0', checked: true, found: 55 },
    ],
};
