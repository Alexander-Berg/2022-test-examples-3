before(function() {

    /* global mock */
    window.mock.signs = [
        {
            params: {},
            data: {
                signs: [
                    {
                        _idx: 0,
                        text: '-- \nplain\nsign',
                        isDefault: false,
                        emails: [],
                        lang: '',
                        userLang: ''
                    },
                    {
                        _idx: 1,
                        text: '<div>--&nbsp;</div><div>html</div><div>sign</div>',
                        isDefault: false,
                        emails: [],
                        lang: '',
                        userLang: ''
                    },
                    {
                        _idx: 2,
                        text: '-- <br/>\nhtml and plain\nsign',
                        isDefault: false,
                        emails: [],
                        lang: '',
                        userLang: ''
                    },
                    {
                        _idx: 3,
                        text: '-- \nstring 1\nstring 2\nstring 3\nstring 4\nsign',
                        isDefault: false,
                        emails: [],
                        lang: '',
                        userLang: ''
                    },
                    {
                        _idx: 4,
                        text: '-- \n&lt;&amp;&gt;\nsign',
                        isDefault: false,
                        emails: [],
                        lang: '',
                        userLang: ''
                    },
                    {
                        _idx: 5,
                        text: '<div>-- \ntest</div>',
                        isDefault: false,
                        emails: [],
                        lang: '',
                        userLang: ''
                    },
                    {
                        _idx: 6,
                        text: '<div>--</div><div>test</div>',
                        isDefault: false,
                        emails: [],
                        lang: '',
                        userLang: ''
                    },
                    {
                        _idx: 7,
                        text: '--\ntest',
                        isDefault: false,
                        emails: [],
                        lang: '',
                        userLang: ''
                    }
                ]
            }
        }
    ];

});
