before(function() {

    window.mock['folders'] = [
        {
            params: {},
            data: {
                'new': 3,
                folder: [
                    {
                        'count': 10,
                        'fid': '1',
                        'new': 2,
                        'subfolder': [],
                        'symbol': 'inbox'
                    },
                    {
                        'count': 1,
                        'fid': '2',
                        'new': 0,
                        'subfolder': [],
                        'symbol': 'spam'
                    },
                    {
                        'count': 5,
                        'fid': '3',
                        'new': 1,
                        'subfolder': [],
                        'symbol': 'archive'
                    },
                    {
                        'count': 1,
                        'fid': '4',
                        'new': 0,
                        'subfolder': [],
                        'symbol': 'draft'
                    },
                    {
                        'count': 1,
                        'fid': '6',
                        'new': 0,
                        'subfolder': [],
                        'symbol': 'template'
                    },
                    {
                        'count': 1,
                        'fid': '7',
                        'new': 0,
                        'subfolder': [],
                        'symbol': 'trash'
                    },
                    {
                        'fid': '8',
                        'subfolder': [],
                        'symbol': 'sent'
                    },
                    {
                        'fid': '9',
                        'subfolder': [],
                        'user': true
                    },
                    {
                        'fid': '10',
                        'subfolder': [],
                        'shared': true
                    },
                    {
                        'fid': '11',
                        'subfolder': [],
                        'user': true
                    },
                    {
                        'count': 1,
                        'fid': '9',
                        'new': 0,
                        'subfolder': [],
                        'symbol': 'outbox'
                    },
                    {
                        'count': 1,
                        'fid': '12',
                        'new': 2,
                        'subfolder': [],
                        'symbol': 'unsubscribe',
                        'user': false
                    }
                ]
            }
        },
        {
            params: {},
            name: 'no_template_symbol',
            data: {
                folder: [
                    {
                        'fid': '4',
                        'subfolder': [],
                        'symbol': 'draft'
                    }
                ]
            }
        },
        {
            params: {},
            name: 'no_template_symbol_and_template_name_inside_drafts',
            data: {
                folder: [
                    {
                        'fid': '4',
                        'subfolder': [],
                        'symbol': 'draft'
                    },
                    {
                        'fid': '41',
                        'parent_id': '4',
                        'subfolder': [],
                        'name': 'template'
                    }
                ]
            }
        },
        {
            params: {},
            name: 'no_template_symbol_and_localized_template_name_inside_drafts',
            data: {
                folder: [
                    {
                        'fid': '4',
                        'subfolder': [],
                        'symbol': 'draft'
                    },
                    {
                        'fid': '411',
                        'parent_id': '4',
                        'subfolder': [],
                        'name': i18n('%Folder_templates')
                    }
                ]
            }
        },

        {
            params: {},
            name: 'father',
            data: {
                folder: [
                    {
                        'fid': '333',
                        'subfolder': [
                            '444',
                            '555'
                        ]
                    },
                    {
                        'fid': '444',
                        'subfolder': [],
                        'parent_id': '333'
                    },
                    {
                        'fid': '555',
                        'parent_id': '333',
                        'subfolder': ['777']
                    },
                    {
                        'fid': '777',
                        'parent_id': '555',
                        'subfolder': ['888']
                    },
                    {
                        'fid': '888',
                        'subfolder': [],
                        'parent_id': '777'
                    }
                ]
            }
        },
        {
            params: {},
            name: 'forGetOpenedFolders',
            data: {
                folder: [
                    {
                        fid: '1',
                        symbol: 'inbox',
                        subfolder: ['2']
                    },
                    {
                        fid: '2',
                        name: 'some_name_1',
                        subfolder: ['3', '4']
                    },
                    {
                        fid: '3',
                        name: 'some_name_2',
                        subfolder: []
                    },
                    {
                        fid: '4',
                        name: 'some_name_3',
                        subfolder: []
                    },
                    {
                        fid: '5',
                        symbol: 'trash',
                        subfolder: []
                    },
                    {
                        fid: '6',
                        symbol: 'draft',
                        subfolder: []
                    }
                ]
            }
        },
        {
            params: {},
            name: 'inbox_with_deeply_nested_subfolders',
            data: {
                folder: [
                    {
                        symbol: 'inbox',
                        name: 'Входящие',
                        folder_options: {
                            position: 0
                        },
                        fid: '1',
                        shared: false,
                        user: false,
                        subfolder: ['20', '16', '7'],
                        level: 0
                    },
                    {
                        name: 'общая папка',
                        folder_options: {
                            position: 0
                        },
                        user: true,
                        fid: '1000',
                        shared: true,
                        subfolder: [],
                        parent_id: '1',
                        level: 1
                    },
                    {
                        name: 'без подпапок',
                        folder_options: {
                            position: 0
                        },
                        user: true,
                        fid: '20',
                        shared: false,
                        subfolder: [],
                        parent_id: '1',
                        level: 1
                    },
                    {
                        name: 'первый уровень вложенности',
                        folder_options: {
                            position: 0
                        },
                        user: true,
                        fid: '16',
                        shared: false,
                        subfolder: ['17'],
                        parent_id: '1',
                        level: 1
                    },
                    {
                        name: 'второй уровень вложенности',
                        parent_id: '16',
                        folder_options: {
                            position: 0
                        },
                        user: true,
                        fid: '17',
                        shared: false,
                        subfolder: ['19'],
                        level: 2
                    },
                    {
                        name: 'третий уровень вложенности',
                        parent_id: '17',
                        folder_options: {
                            position: 0
                        },
                        user: true,
                        fid: '19',
                        shared: false,
                        subfolder: [],
                        level: 3
                    },
                    {
                        symbol: 'archive',
                        name: 'Архив',
                        folder_options: {
                            position: 0
                        },
                        fid: '7',
                        shared: false,
                        user: true,
                        subfolder: [],
                        parent_id: '1',
                        level: 1
                    }
                ]
            }
        }
    ];

});
