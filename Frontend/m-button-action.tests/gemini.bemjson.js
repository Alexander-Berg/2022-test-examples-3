({
    block: 'b-page',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {block: 'i-jquery', mods: {version: 'default'}},
        {elem: 'js', url: '_gemini.js'}
    ],
    content: {
        block: 'gemini',
        attrs: {style: 'padding: 40px'},
        content: {
            block: 'm-button-action',
            mods: {type: 'email'},
            jabberSt: 'offline',
            content: 'x@yandex-team.ru'
        }
    }
});
