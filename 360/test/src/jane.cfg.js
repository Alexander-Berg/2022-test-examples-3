(function() {
    const renderInsertJsScript = () => {
        const json = JSON.stringify({
            api: {
                'models': '/web-api/models/liza1',
                'upload-signature-images': '/web-api/upload-signature-images/liza1',
                'upload-attachment': '/web-api/upload-attachment/liza1'
            },

            prefetched: {
                models: {}
            },

            UA: {
                BrowserName: 'Chrome'
            },

            Config: {
                freeze: {
                    paths: {},
                    entries: {},
                },
                connection_id: 'abc123',
                'domain-options': {
                    region: 'RU'
                },
                dev: false,
                layout: '',
                locale: 'ru',
                product: '',
                'help-url': 'https://yandex.ru/support/mail',
                domainZonesForReg: 'com\\.am|az|by|com|ee|fr|com\\.ge|co\\.il|kg|kz|lt|lv|md|ru|tj|tm|com\\.tr|ua|uz',
                PDD: {
                    domain: false
                },
                pddDomain: false,
                workspace: false,
                XSL: {},
                'yandex-domain': 'yandex.ru',
                region_parents: [],
                'staff-host': 'http://staff.yandex-team.ru',
                firstLogin: null,
                'exp-boxes': '',
                'eexp-boxes': '',
                'exp-test-ids': [],
                'new-themes': [],
                'input-multiple': false,
                service: 'LIZA',
                version: 'VER',
                constants: {
                    WIDGET_SHOW_TYPES: {
                        LIST_TYPE: 'list'
                    }
                }
            },
            lcn: 0,
            locales: [],
            uid: '12345678901234567890',
            urlParams: {},
            IS_KCUF: false,
            IS_CORP: false,
            SIDS: []
        });

        const script = document.createElement('script');
        script.id = 'insert-js';
        script.type = 'application/json';
        script.innerText = json;
        document.head.appendChild(script);
    };

    const renderStatuslineDiv = () => {
        const statuslineDiv = document.createElement('div');
        statuslineDiv.className = 'js-statusline';
        document.body.appendChild(statuslineDiv);
    };

    renderInsertJsScript();
    renderStatuslineDiv();
})();

window.global = window.global || window;
