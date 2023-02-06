module.exports = {
    type: 'wizard',
    request_text: 'блендер',
    exp_flags: ['misspell_background'],
    data_stub: [{
        wizplace: 0,
        type: 'web_misspell',
        kind: 'merged',
        counter_prefix: '/wiz/web_misspell/misspell/',
        '@id': 'wiz.web_misspell.p0.ik',
        types: {
            kind: 'wizard'
        },
        misspell: {
            items: [{
                raw_text: 'бленде\u0007[р\u0007]',
                clear_text: 'блендер'
            }]
        }
    }]
};
