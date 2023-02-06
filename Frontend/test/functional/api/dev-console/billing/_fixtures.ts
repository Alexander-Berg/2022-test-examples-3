/* eslint-disable */
export const merchantsFixture = [
    {
        deleted: false,
        enabled: true,
        organization_name: 'Yandex',
        token: '52357983-efb2-47b3-a2c2-57e616243633',
        entity_id: 'alice-ext-skill:d5801da5-0c3b-4ccd-8c11-bd0bfdc5e880',
        description:
            "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500",
    },
    {
        deleted: false,
        enabled: false,
        organization_name: 'Yandex',
        token: '52357983-efb2-47b3-a2c2-57e616243633',
        entity_id: 'alice-ext-skill:d5801da5-0c3b-4ccd-8c11-bd0bfdc5e881',
        description:
            "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500",
    },
    {
        deleted: true,
        enabled: false,
        organization_name: 'Yandex',
        token: '52357983-efb2-47b3-a2c2-57e616243633',
        entity_id: 'alice-ext-skill:d5801da5-0c3b-4ccd-8c11-bd0bfdc5e882',
        description: '',
    },
];

export const incorrectMerchantsFixture1 = [
    {
        // missing id
        // description is not a string
        deleted: true,
        enabled: false,
        organization_name: 'Yandex',
        token: '52357983-efb2-47b3-a2c2-57e616243633',
        description: { value: '' },
    },
];

export const incorrectMerchantsFixture2 = [null, 'string'];

export const merchantFixture = {
    deleted: true,
    enabled: false,
    organization_name: 'Yandex',
    token: '52357983-efb2-47b3-a2c2-57e616243633',
    entity_id: 'alice-ext-skill:d5801da5-0c3b-4ccd-8c11-bd0bfdc5e883',
    description:
        "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500",
};

export const incorrectMerchantFixture = {
    // missing id
    // description is not a string
    deleted: true,
    enabled: false,
    organization_name: 'Yandex',
    token: '52357983-efb2-47b3-a2c2-57e616243633',
    description: { value: '' },
};
