export const ourOperatorId = 1;
export const otherOperatorId = 2;
export const ourOrgId = 3;

export const clientInfoFields: Api.ClientInfoField[] = [
    {
        key: 'phone',
        label: 'key1',
        editable: true,
        value: 'key1',
        type: 'string',
    },
    {
        key: 'email',
        label: 'key2',
        editable: true,
        value: '8-888-888-88-88',
        type: 'phone',
    },
    {
        key: 'name',
        label: 'key3',
        editable: true,
        value: 'a@a.a',
        type: 'email',
    },
];

export const clientInfo = {
    defaults: {
        crm: 'defaults',
        key: 'defaults',
        label: 'defaults',
        version: 0,
        fields: [clientInfoFields[0], clientInfoFields[1]],
    },
    another: {
        crm: 'another',
        key: 'another',
        label: 'another',
        version: 'another',
        fields: [clientInfoFields[2]],
    },
};

export const crmOrder = ['defaults', 'another'];

export const clientInfoState = {
    clientInfo: {
        byChatId: {
            '0/4/1': {
                clientInfo,
                crmOrder,
            },
        },
    },

};
