
export const CHILDREN_GOODS_CATEGORY = {
    name: 'Детские товары',
    uniqName: 'Детские товары',
    hid: 90764,
    nid: 54421,
    slug: 'detskie-tovary',
};


export default {
    data: {
        intents: [90764, 91461],
    },
    collections: {
        intent: {
            90764: {
                category: CHILDREN_GOODS_CATEGORY,
                defaultOrder: 1,
            },
            91461: {
                category: {
                    hid: 91461,
                    name: 'Телефоны',
                    nid: 54437,
                    slug: 'telefony-i-aksessuary-k-nim',
                    uniqName: 'Телефоны и аксессуары к ним',
                },
                defaultOrder: 8,
                ownCount: 10,
            },
        },
    },
};
