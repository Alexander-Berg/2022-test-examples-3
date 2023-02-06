before(function() {

    mock['threads-info'] = [
        {
            params: {
                thread_ids: '52,54'
            },
            data: [ {
                tid: 't52',
                new: 5
            },  {
                tid: 't54',
                new: 1
            } ]
        },
        {
            params: {
                thread_ids: '52,53,54'
            },
            data: [ {
                tid: 't52',
                new: 5
            },  {
                tid: 't54',
                new: 1
            } ]
        }
    ];

});
