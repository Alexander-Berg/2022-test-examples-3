({
    block: 'x-page',
    title: 'mi-jabber',
        head: [
        {elem: 'css', url: '_gemini.css', ie: false}
    ],
    content: {
        block: 'gemini',
        content: ['available', 'chat', 'away',
                  'xa', 'dnd', 'notinlist',
                  'offline', 'unavailable', 'none']
                        .map(status => [{block: 'mi-jabber', mods: {status}}, status, {tag: 'br'}])
    }
});
