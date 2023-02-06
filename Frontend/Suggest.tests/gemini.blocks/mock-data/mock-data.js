(function(undefined) {
    var mockData = {
        'short': [
            'мороз и солнце день чудесный',
            [
                'мороз и солнце день чудесный',
                'мороз и солнце день чудесный стих',
                'мороз и солнце день чудесный пушкин стих',
                'мороз и солнце день чудесный еще ты дремлешь друг прелестный',
                'мороз и солнце день чудесный слушать',
                'мороз и солнце день чудесный анализ стихотворения',
                'мороз и солнце день чудесный на английском языке'
            ],
            {r: 1, log: 'sgtype:BTTWWWB', 'continue': 'ещё ты дремлешь'}
        ],
        'long': [
            'смотреть фильмы онлайн в высоком качестве hd без регистрации и смс новинки',
            [
                [
                    '',
                    'смотреть фильмы онлайн в высоком качестве hd без регистрации и смс но mock',
                    {word_complete: 1}
                ]
            ],
            {r: 1, log: 'sgtype:In'}
        ]
    };

    /**
     * Поставщик mock-данных для саджеста.
     * Элемент переопределяет `input__dataprovider`
     */
    BEM.decl({name: 'mock-data', baseBlock: 'suggest__provider'}, {

        /**
         * Запрашивает подсказки для саджеста.
         *
         * @param {String} part Строка запроса
         * @param {Number} pos Позиция курсора
         * @param {Function} callback Обработчик полученных данных
         */
        get: function(part, pos, callback) {
            if(document.location.search.indexOf('mock') !== -1) {
                if(part.length < 1) {
                    return;
                }

                var mockVariant = part.length < mockData['long'][0].length ? 'short' : 'long';

                callback({items: mockData[mockVariant][1], metainfo: mockData[mockVariant][2]});
            } else {
                return this.__base(part, pos, callback);
            }
        },

        /**
         * Установка mock-данных.
         *
         * @param {String} key
         * @param {Array} value
         * @returns {this}
         */
        setMock: function(key, value) {
            mockData[key] = value;
            return this;
        }

    });
})();
