import {map, times} from 'ambar';

const getChild = id => ({
    'category': {
        entity: 'category',
        fullName: 'Моноподы и пульты для селфи',
        slug: 'monody-i-pulty-dlia-selfi',
        id,
        isLeaf: true,
        modelsCount: 18,
        name: `Моноподы и пульты для селфи ${id}`,
        nid: id,
        offersCount: 2367,
        type: 'guru',
        viewType: 'list',
    },
    'childrenType': 'guru',
    'entity': 'navnode',
    'fullName': `Мобильные телефоны ${id}`,
    'name': `Мобильные телефоны ${id}`,
    'hasPromo': false,
    id,
    'isLeaf': true,
    'rootNavnode': {
        'entity': 'navnode',
        'id': 54432,
    },
    'slug': 'mobilnye-telefony',
    'type': 'category',
    'link': {
        params: {
            'hid': ['91032'],
            'nid': [id],
        },
        target: 'catalogleaf',
    },
});

const ids = times(idx => 10222 + idx, 10);
const level3Nodes = map(id => getChild(id), ids);

const level2Node = {
    'childrenType': 'guru',
    'entity': 'navnode',
    'fullName': 'Чепчики для малышей',
    'id': 73321,
    'isLeaf': false,
    'name': 'Проверяемый текст',
    'rootNavnode': {
        'entity': 'navnode',
        'id': 54432,
    },
    'slug': 'chepchiki-dlia-malyshei',
    'type': 'guru_recipe',
    'link': {
        'params': {
            'nid': [
                73321,
            ],
        },
        'target': 'catalog',
    },
    'navnodes': level3Nodes,
};

const level1Node = {
    'childrenType': 'guru',
    'entity': 'navnode',
    'fullName': 'Аксессуары для малышей',
    'id': 73320,
    'slug': 'aksessuary-dlia-malyshei',
    'isLeaf': false,
    'type': 'virtual',
    'name': 'Аксессуары',
    'navnodes': [level2Node],
};

export {
    level2Node,
    level1Node,
};
