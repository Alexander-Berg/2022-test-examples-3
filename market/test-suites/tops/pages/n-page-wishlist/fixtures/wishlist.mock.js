import {random} from 'lodash';

const randomId = () => String(random(0, 100000));

export const createWishlistItemBySku = ({id, references}) => ({
    id: randomId(),
    owner_id: 1584874,
    rgb: 'white',
    reference_type: 'sku',
    reference_id: id,
    title: 'Подтяжки мужские MOKKI черные',
    image_base_url:
        'https://avatars.mds.yandex.net/get-mpic/5207395/img_id8393124735082110991.jpeg/6hq',
    price: {currency: 'RUR', amount: 759},
    added_at: '2022-07-08T13:05:38.796757Z',
    secondary_references: [
        {type: 'hid', id: references.hid},
        {type: 'productId', id: references.productId},
    ],
    regionId: 213,
});

export const createWishlistItem = offerId => ({
    items: [{
        id: 101684014,
        owner_id: 3884,
        rgb: 'white',
        reference_type: 'offer',
        reference_id: offerId,
        title: 'Смартфон 4Good Clab Test Phone teso2 Голубой (Зоопарк)',
        image_base_url: '//avatars.mds.yandex.net/get-mpic/1928572/f8ff5708-0d53-4b38-b813-112c8ec64902/orig',
        price: {
            currency: 'RUR',
            amount: 9081,
        },
        added_at: '2020-11-17T10:59:56.433421Z',
        secondary_references: [
            {
                type: 'hid',
                id: '91491',
            },
            {
                type: 'modelId',
                id: '142798000',
            },
        ],
        regionId: 213,
    }],
    token: {
        lastCrTime: '2020-11-17T10:59:56.433421Z',
        lastId: null,
    },
    hasMore: false,
});

export const createWishlistState = (items = []) => ({
    items,
    token: 'token',
    hasMore: true,
});
