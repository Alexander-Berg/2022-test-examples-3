import {map, times} from 'ambar';

const getChild = id => ({
    'category': {
        entity: 'category',
        fullName: 'Моноподы и пульты для селфи',
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

const ids = times(idx => 10222 + idx, 3);
const level3Nodes = map(id => getChild(id), ids);

const virtualNode = {
    'childrenType': 'guru',
    'entity': 'navnode',
    'fullName': 'Чепчики для малышей',
    'id': 73321,
    'isLeaf': false,
    'hasPromo': false,
    'name': 'Проверяемый текст',
    'rootNavnode': {
        'entity': 'navnode',
        'id': 54432,
    },
    'slug': 'chepchiki-dlia-malyshei',
    'type': 'virtual',
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

const parentOfVirtualNode = {
    'childrenType': 'guru',
    'entity': 'navnode',
    'fullName': 'Аксессуары для малышей',
    'id': 73320,
    'slug': 'aksessuary-dlia-malyshei',
    'isLeaf': false,
    'type': 'virtual',
    'name': 'Аксессуары',
    'navnodes': [virtualNode],
};

export {
    virtualNode,
    parentOfVirtualNode,
};
