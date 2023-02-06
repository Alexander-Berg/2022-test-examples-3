([{
    block: 'b-page',
    title: 'm-notification',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        (function() {
            var themes = ['info', 'error', 'warning', 'success'],
                icons = ['', 'yes'],
                clear = ['', 'yes'];

            return themes.map(function(theme) {
                return icons.map(function(icon) {
                    return clear.map(function(clear) {
                        return {
                            block: 'test',
                            js: true,
                            mix: {block: 'test-' + theme +
                                (icon === 'yes' ? '-icon' : '') +
                                (clear === 'yes' ? '-clear' : '')},
                            content: {
                                block: 'm-notification',
                                mods: {theme: theme, icons: icon, clear: clear},
                                content: 'Уведомление'
                            }
                        };
                    });
                }).concat({tag: 'br'});
            });
        })(),
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
}]);
