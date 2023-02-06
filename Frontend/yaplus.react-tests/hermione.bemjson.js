({
    block: 'x-page',
    content: [
        {
            block: 'hermione',
            attrs: { id: 'simple' },
            content: {
                block: 'yaplus',
            },
        },
        {
            block: 'hermione',
            attrs: { id: 'available' },
            content: {
                block: 'yaplus',
                mods: {
                    available: 'yes',
                },
            },
        },
        {
            block: 'hermione',
            attrs: { id: 'custom' },
            content: {
                block: 'yaplus',
                mods: {
                    type: 'new',
                    available: 'yes',
                },
                headerText: 'header',
                mainDescription: 'main',
                leftButtonText: 'left',
                rightButtonText: 'right',
            },
        },
    ],
});
