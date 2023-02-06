before(function() {

    window.mock['labels'] = [
        {
            params: {},
            data: {
                label: [
                    {
                        name: 'test_label_name',
                        count: 1,
                        user: true,
                        lid: '1'
                    },
                    {
                        name: 'test_label_name1',
                        count: 2,
                        lid: '2',
                        symbolicName: 'test_symbol'
                    },
                    {
                        name: 'test_label_name2',
                        count: 3,
                        user: true,
                        lid: '3'
                    },
                    {
                        name: 'Важное',
                        count: 3034,
                        lid: '123',
                        symbolicName: 'important_label'
                    },
                    {
                        name: 'SystMetkaSO:delivery',
                        count: 4,
                        lid: '4'
                    },
                    {
                        name: 'yasupport',
                        count: 1,
                        lid: '5'
                    },
                    {
                        name: 'vtnrf0habrahabr',
                        count: 319,
                        social: true,
                        title: 'Все письма от сайта Хабрахабр',
                        lid: '2170000000007984852'
                    },
                    {
                        name: 'remindme_threadabout:mark',
                        count: 1,
                        default: true,
                        lid: '2480000000552883262'
                    },
                    {
                        name: 'not_show_widget_event',
                        lid: '2370000000112026049'
                    },
                    {
                        name: '_pinned_',
                        count: 9,
                        default: true,
                        lid: '2420000001823639879',
                        symbolicName: 'pinned_label'
                    }
                ]
            }
        }
    ];

});
