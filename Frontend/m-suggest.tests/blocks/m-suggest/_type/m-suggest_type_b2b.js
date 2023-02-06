BEM.DOM.decl({block: 'm-suggest'}, {
    _sendRequest: function() {
        this._processResult([{
            pagination: {
                per_page: 5,
                page: 0,
                pages: 1
            },
            layer: 'groups',
            result: [{
                    fields: [
                        {type: 'slug', value: 'female'},
                        {type: 'type', value: 'wiki'},
                        {type: 'service', value: null},
                        {type: 'department', value: null}
                    ],
                    layer: 'groups',
                    id: '1634',
                    title: 'Некое название группы'
                },
                {
                    fields: [
                        {type: 'slug', value: 'female'},
                        {type: 'type', value: 'staff'},
                        {type: 'service', value: null},
                        {type: 'department', value: null}
                    ],
                    layer: 'groups',
                    id: '1634',
                    title: 'Название группы'
                }
            ]
        }]);
    }
});
