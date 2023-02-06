const filter = {
    id: '7893318',
    type: 'enum',
    name: 'Производитель',
    subType: '',
};

export const enumFilter = {
    ...filter,
    values: [
        {
            found: 15,
            value: 'APLUS',
            id: '13957595',
        },
        {
            found: 1,
            value: 'AVON',
            id: '997863',
        },
        {
            found: 6,
            value: 'Bridgestone',
            id: '152786',
        },
    ],
    valuesGroups: [
        {
            type: 'all',
            valuesIds: [
                '13957595',
                '997863',
                '152786',
            ],
        },
        {
            type: 'top',
            valuesIds: [
                '997863',
            ],
        },
    ],
};

export const enumFilterWithSelectedVal = {
    ...filter,
    values: [
        {
            found: 15,
            value: 'APLUS',
            checked: true,
            id: '13957595',
        },
        {
            found: 1,
            value: 'AVON',
            checked: true,
            id: '997863',
        },
        {
            found: 6,
            value: 'Bridgestone',
            id: '152786',
        },
    ],
    valuesGroups: [
        {
            type: 'all',
            valuesIds: [
                '13957595',
                '997863',
                '152786',
            ],
        },
    ],
};

export const enumFilterColor = {
    ...filter,
    values: [
        {
            found: 15,
            value: 'черный',
            code: '#000000',
            id: '13957595',
        },
    ],
    valuesGroups: [
        {
            type: 'all',
            valuesIds: [
                '13957595',
            ],
        },
    ],
};
