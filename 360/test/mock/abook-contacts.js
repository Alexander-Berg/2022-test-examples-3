before(function() {

    window.mock['abook-contacts'] = [
        {
            params: {},
            data: {}
        },
        {
            params: {
                emails: 'test@ya.ru',
                pagesize: 30
            },
            data: {
                items: []
            }
        }
    ];

});
