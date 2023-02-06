({
    block: 'b-page',
    title: 'country-flag',
    head: [{
        elem: 'css',
        url: '_gemini.css',
        ie: false
    }, {
        elem: 'css',
        url: '_gemini',
        ie: true
    }],
    content: [
        ['s16', 's32', 's64'].map(function(size) {
            return {
                content: [{
                    block: 'gemini-size-title',
                    tag: 'h2',
                    content: 'size: ' + size
                }, {
                    block: 'gemini-flag-block',
                    content: [{
                        block: 'gemini-flag-' + size,
                        content: [
                            ['ab', 'au', 'at', 'ak', 'az', 'as', 'ax', 'al', 'dz', 'ai', 'en', 'england',
                                'ao', 'ad', 'ag', 'ae', 'ar', 'am', 'aw', 'af', 'bs', 'bd', 'bb', 'bh', 'bz',
                                'by', 'be', 'bj', 'bm', 'bg', 'bo', 'bon', 'ba', 'bw', 'br', 'io', 'bn', 'bf',
                                'bi', 'bt', 'vu', 'va', 'gb', 'hu', 've', 'vg', 'vi', 'tl', 'vn', 'ga', 'hi',
                                'ht', 'gy', 'gm', 'gh', 'gp', 'gt', 'gn', 'gw', 'de', 'gus', 'gi', 'hn', 'hk',
                                'gd', 'gl', 'gr', 'ge', 'gu', 'dk', 'dj', 'dm', 'do', 'eg', 'zm', 'eh', 'zw',
                                'il', 'in', 'id', 'jo', 'iq', 'ir', 'ie', 'is', 'es', 'it', 'ye', 'cv', 'kz',
                                'ky', 'kh', 'cm', 'ca', 'cis', 'qa', 'ke', 'cy', 'kg', 'ki', 'cn', 'cc', 'co',
                                'km', 'cg', 'cr', 'ci', 'cu', 'kw', 'cur', 'la', 'lv', 'ls', 'lr', 'lb', 'ly',
                                'lt', 'li', 'lu', 'mu', 'mr', 'me', 'yt', 'mo', 'mk', 'mw', 'my', 'ml', 'mv', 'mt',
                                'ma', 'mq', 'mh', 'mx', 'mi,fm', 'mz', 'md', 'mc', 'mn', 'ms', 'mm', 'nk', 'na', 'nr',
                                'np', 'ne', 'ng', 'nl', 'ni', 'nu', 'nz', 'nc', 'no', 'nf', 're', 'ck', 'tc', 'om',
                                'jy', 'mai', 'im', 'cx', 'sab', 'sh', 'pk', 'pw', 'ps', 'pa', 'pg', 'py', 'pe', 'pn',
                                'pl', 'pt', 'dmr', 'pr', 'cd', 'rk', 'ru', 'rw', 'ro,s v', 'ws', 'sm', 'st', 'sa',
                                'smn', 'sz', 'nd', 'kp', 'mp', 'sc', 'sbi', 'vc', 'pm', 'sn', 'kn', 'lc', 'rs', 'cs',
                                'ct', 'sg', 'smf', 'ses', 'sy', 'sk', 'si', 'sb', 'so', 'sd', 'sr', 'us', 'sl', 'tj',
                                'th', 'tz', 'tg', 'tk', 'to', 'trin', 'tv', 'tn', 'tm', 'tr', 'ug', 'uz', 'ua', 'wf',
                                'uy', 'we,fo', 'fj', 'ph', 'fi', 'fk', 'fr', 'gf', 'pf', 'hr', 'cf', 'td', 'cz', 'cl',
                                'ch', 'se', 'scotland', 'sj', 'lk', 'ec', 'gq', 'er', 'ee', 'et', 'za', 'gs',
                                'kr', 'os', 'ss', 'jm', 'jp'
                            ].map(function(flagCode) {
                                var mods = {};
                                mods[size] = flagCode;
                                return {
                                    tag: 'span',
                                    content: [{
                                            block: 'country-flag',
                                            mods: mods
                                        },
                                        '\u00a0\u00a0'
                                    ]
                                };
                            })
                        ]
                    }]
                }]
            };
        }),
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {elem: 'js', url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'}
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {block: 'i-jquery', mods: {version: '1.8.3'}}
        },
        {
            elem: 'cc',
            condition: 'gt IE 8',
            others: true,
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_gemini.js'}
    ]
});
