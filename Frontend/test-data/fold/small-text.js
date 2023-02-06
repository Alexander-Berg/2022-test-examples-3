const data = require('../../tools/data');

module.exports = data.createSnippet({
    content_type: 'markup',
    content: {
        content_type: 'fold',
        content: [
            {
                content_type: 'paragraph',
                content: [
                    'Ганиме́д (др.-греч. Γανυμήδης) — один из ',
                    {
                        content_type: 'link',
                        url: '/',
                        content: 'галилеевых спутников',
                    },
                    ' Юпитера, cедьмой по расстоянию от него среди всех его спутников и крупнейший спутник в Солнечной системе. Его диаметр равен 5268 километрам, что на 2 % больше.',
                ],
            },
        ],
    },
});
