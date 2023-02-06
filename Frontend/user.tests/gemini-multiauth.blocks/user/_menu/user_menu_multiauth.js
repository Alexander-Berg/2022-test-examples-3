BEM.decl({name: 'user_menu_multiline__provider_one_user', baseBlock: 'i-request_type_ajax'}, {
    get: function(data, cb) {
        cb({
            accounts: [
                {
                    uid: '4000935005',
                    login: 'constantine',
                    displayName: {
                        name: 'constantine',
                        default_avatar: '4000935005'
                    },
                    attributes: {}
                }
            ],
            default_uid: '4000935005',
            'can-add-more': true
        });
    }
});

BEM.decl({name: 'user_menu_multiline__provider_many_users', baseBlock: 'i-request_type_ajax'}, {
    get: function(data, cb) {
        cb({
            accounts: [
                {
                    uid: '4000935005',
                    login: 'constantine',
                    displayName: {
                        name: 'constantine',
                        default_avatar: '4000935005'
                    },
                    attributes: {}
                },
                {
                    uid: '4000935385',
                    login: 'rokossovsky',
                    displayName: {
                        name: 'rokossovsky',
                        default_avatar: '4000935385'
                    },
                    attributes: {}
                }
            ],
            default_uid: '4000935005',
            'can-add-more': true
        });
    }
});
