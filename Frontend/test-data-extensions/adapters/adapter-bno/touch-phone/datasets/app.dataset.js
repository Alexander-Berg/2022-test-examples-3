var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = function() {
    return {
        app: {
            thumb: {
                image: stubs.imageStub(72, 72),
                width: 72,
                height: 72
            },
            title: 'R-Mobile - лучший мобильный банк',
            rating: { value: 4, votes: 1934 },
            button: {
                text: 'Загрузить',
                url: 'https://itunes.apple.com/app/id557857165'
            },
            indented: true
        }
    };
};
