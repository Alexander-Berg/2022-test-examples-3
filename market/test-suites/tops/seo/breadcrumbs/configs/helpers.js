function getCatalogerMock(mock) {
    return {
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
        'id': 54415,
        'isLeaf': false,
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
        'navnodes': [mock],
    };
}

export {
    getCatalogerMock,
};
