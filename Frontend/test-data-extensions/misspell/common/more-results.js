module.exports = {
    type: 'wizard',
    request_text: '24videonet',
    data_stub: [{
        applicable: 1,
        counter_prefix: '/wiz/web_misspell/misspell/',
        kind: 'merged',
        misspell: {
            applicable: 1,
            counter_prefix: '/wiz/misspell/',
            items: [
                {
                    clear_text: '24 video net',
                    dist: '3',
                    flags: 64,
                    force: 0,
                    from: 'balancer',
                    orig_penalty: '0',
                    raw_source_text: '24videonet',
                    raw_text: '24 video net',
                    relev: 8000,
                    source: 'Misspell',
                    weight: 18
                }
            ],
            orig_weight: 22,
            relev: 1.1,
            source: 'balancer',
            subtype: [
                'misspell'
            ],
            type: 'misspell_source',
            types: {
                all: [
                    'wizard',
                    'misspell'
                ],
                extra: [],
                kind: 'wizard',
                main: 'misspell'
            },
            use_balancer: 1,
            wizplace: 'upper'
        },
        package: 'YxWeb::Wizard::WebMisspell',
        relev: 1,
        remove: 'switch_off_thes',
        subtype: [
            'misspell'
        ],
        type: 'web_misspell',
        types: {
            all: [
                'wizard',
                'web_misspell',
                'misspell'
            ],
            extra: [
                'misspell'
            ],
            kind: 'wizard',
            main: 'web_misspell'
        },
        wizplace: 'upper'
    }]
};
