describeBlock('adapter-lyrics__mark-bold', block => {
    let context;
    let cite;

    beforeEach(() => {
        context = {};
    });

    it('should correctly replace marks', () => {
        const expected = [
            [
                'Послушайте!',
                'Ведь, \u0007[если\u0007] \u0007[звезды\u0007] \u0007[зажигают\u0007] -',
                '\u0007[значит\u0007] - \u0007[это\u0007] \u0007[кому\u0007]-\u0007[нибудь\u0007] \u0007[нужно\u0007]?',
                'Значит - кто-то хочет, чтобы они были?',
                'Значит - кто-то называет эти плевочки',
                'жемчужиной?'
            ],
            [
                'И, надрываясь',
                'в метелях полуденной пыли,'
            ]
        ];

        /*eslint-disable */
        cite = {
            lines: [
                { style: '', text: 'Послушайте!' },
                { style: '', text: 'Ведь, если звезды зажигают -' },
                { style: '', text: 'значит - это кому-нибудь нужно?' },
                { style: '', text: 'Значит - кто-то хочет, чтобы они были?' },
                { style: '', text: 'Значит - кто-то называет эти плевочки' },
                { style: '', text: 'жемчужиной?' },
                { style: '', text: '' },
                { style: '', text: 'И, надрываясь' },
                { style: '', text: 'в метелях полуденной пыли,' }
            ],
            linesWithMarker: [
                { style: '', text: 'Послушайте!' },
                { style: '', text: 'Ведь, %%endhl%%если%%hl%% %%endhl%%звезды%%hl%% %%endhl%%зажигают%%hl%% -' },
                { style: '', text: '%%endhl%%значит%%hl%% - %%endhl%%это%%hl%% %%endhl%%кому%%hl%%-%%endhl%%нибудь%%hl%% %%endhl%%нужно%%hl%%?' },
                { style: '', text: 'Значит - кто-то хочет, чтобы они были?' },
                { style: '', text: 'Значит - кто-то называет эти плевочки' },
                { style: '', text: 'жемчужиной?' },
                { style: '', text: '' },
                { style: '', text: 'И, надрываясь' },
                { style: '', text: 'в метелях полуденной пыли,' }
            ]
        };
        /*eslint-enable */

        assert.deepEqual(block(context, cite), expected);
    });
});
