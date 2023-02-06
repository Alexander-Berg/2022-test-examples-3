const getCatalogerMock = navnodes => ({
    'category': {
        'entity': 'category',
        'id': 90401,
        'isLeaf': false,
        'modelsCount': 4866521,
        'name': 'Все товары',
        'nid': 54415,
        'offersCount': 110847388,
    },
    'childrenType': 'mixed',
    'entity': 'navnode',
    'fullName': 'Все товары',
    'hasPromo': false,
    'id': 54432,
    'isLeaf': false,
    'rootNavnode': {
        'entity': 'navnode',
        'id': 54432,
    },
    'link': {
        'params': {
            'hid': [
                '90401',
            ],
            'nid': [
                '54415',
            ],
        },
        'target': 'catalog',
    },
    'name': 'Все товары',
    'slug': 'vse-tovary',
    'type': 'category',
    navnodes,
});

export {
    getCatalogerMock,
};
