before(function() {

    /* global mock */
    window.mock['settings'] = [
        {
            params: {},
            data: {
                test_has_setting: null,
                test_number: 1,
                test_object: "{}",
                'last-time-show-promo': 0,
                folder_thread_view: 'on',
                'last-promo-was-name': 'gb_present',
                a: 'on',
                b: 1,
                c: 2,
                z: '',
                default_email: 'doochik@yandex.ru',
                reply_to: ['doochik@yandex.ru', 'my@ya.ru'],
                emails: [
                    {'native': true, validated: true, address: 'doochik@yandex.ru'},
                    {'native': false, validated: true, address: 'my@ya.ru'}
                ],

                // update-settings mock
                'test-empty-name': {},

                'test-non-empty-name': encodeURIComponent(JSON.stringify({
                    alreadyBeHere: true
                })),
                'setting-with-inner-obj': encodeURIComponent(JSON.stringify({
                    innerObj: {
                        foo: 'bar'
                    }
                })),
                'number-name': 123,
                'string-name': 'rare-pink-unicorn',
                'array-name': ['sonic-the-hedgehog', 'nyan-cat', 'Bender Bending Rodriguez']
            }
        },
        {
            params: {},
            name: 'settings_conf1',
            data: {
                'size-view-app': '20',
                'size-layout-left': '5'
            }
        },
        {
            params: {},
            name: 'settings_enable_hotkeys1',
            data: {
                'enable_hotkeys': true
            }
        },
        {
            params: {},
            name: 'settings_enable_hotkeys2',
            data: {
                'enable_hotkeys': false
            }
        },
        {
            params: {},
            name: 'settings_enable_timeline',
            data: {
                'timeline-is-open': true,
                'timeline_enable': true
            }
        },
        {
            params: {},
            name: 'settings_disable_timeline',
            data: {
                'timeline-is-open': false,
                'timeline_enable': true
            }
        },
        {
            params: {},
            name: 'right_column_expanded_true',
            data: {
                'right_column_expanded': true
            }
        },
        {
            params: {},
            name: 'right_column_expanded_false',
            data: {
                'right_column_expanded': false
            }
        }
    ];

});
