const wineCategory = {
    entity: 'category',
    fullName: 'Вино',
    id: 16155466,
    isLeaf: true,
    kinds: [
        'alco',
    ],
    name: 'Вино',
    nid: 82914,
    type: 'gurulight',
};

export default {
    categories: [wineCategory],
    navnodes: [{
        entity: 'navnode',
        category: wineCategory,
        id: 82914,
        slug: 'vino',
    }],
    titles: {
        raw: 'Самогон',
        highlighted: [{
            value: 'Самогон',
        }],
    },
    slug: 'samogon',
    id: 100500,
    type: 'model',
    description: 'test',
    lingua: {
        type: {nominative: '', genitive: '', dative: '', accusative: ''},
    },
    specs: {
        friendly: ['домашний самогон'],
        friendlyext: [{
            type: 'spec',
            value: 'домашний самогон',
            usedParams: [16156105],
        }],
    },
    opinions: 25,
};
