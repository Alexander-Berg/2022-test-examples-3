({
    block: 'x-page',
    title: 'm-orange',
    content: {
        block: 'gemini',
        content: {
            block: 'gemini-inner',
            content: [
                {
                    block: 'm-orange',
                    js: {parentOffset: 'gemini-inner'}
                },
                {tag: 'br'},
                {
                    block: 'm-orange',
                    js: {parentOffset: 'gemini-inner'},
                    mods: {'new': 'notifications'}
                }
            ]
        }
    }
});
