const medsCategory = {
    entity: 'category',
    fullName: 'Средства для лечения боли',
    id: 15756503,
    isLeaf: false,
    kinds: [
        'medicine',
    ],
    name: 'Лечение боли',
    nid: 72466,
    type: 'guru',
};

export default {
    categories: [medsCategory],
    navnodes: [{
        entity: 'navnode',
        category: medsCategory,
        id: 72466,
        slug: 'lechenie-boli',
    }],
    titles: {
        raw: 'Парацетамол сусп. внутр. клубника 120мг/5мл 100мл №1',
        highlighted: [{
            value: 'Парацетамол сусп. внутр. клубника 120мг/5мл 100мл №1',
        }],
    },
    slug: 'paratsetamol-susp-vnutr-klubnika-120mg-5ml-100ml-1',
    id: 100500,
    type: 'model',
    description: 'test',
    lingua: {
        type: {nominative: '', genitive: '', dative: '', accusative: ''},
    },
    specs: {
        friendly: ['лекарственный препарат'],
        friendlyext: [{
            type: 'spec',
            value: 'лекарственный препарат',
            usedParams: [17641866],
        }],
    },
    opinions: 25,
    prices: {
        min: '10',
        currency: 'RUR',
    },
};
