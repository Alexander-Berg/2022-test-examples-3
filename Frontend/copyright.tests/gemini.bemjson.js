({
    block: 'x-page',
    content: [
        {
            block: 'gemini',
            mix: {block: 'gemini-simple'},
            content: {block: 'copyright', start: 2005}
        },
        {
            block: 'gemini',
            mix: {block: 'gemini-no-url'},
            content: {block: 'copyright', start: 2005, url: false}
        },
        {
            block: 'gemini',
            mix: {block: 'gemini-custom-text'},
            content: {
                block: 'copyright',
                start: 2005,
                content: 'Компания ответственных профессионалов'
            }
        }
    ]
});
