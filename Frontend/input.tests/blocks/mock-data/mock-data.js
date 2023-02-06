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
    BEM.decl({name: 'mock-data', baseBlock: 'input__dataprovider'}, {

        /**
         * Запрашивает подсказки для саджеста.
         *
         * @public
         * @param {String}            request  Часть текста, для которой нужно запросить данные.
         * @param {inputDataRecieved} callback Метод, который будет вызван после успешного получения данных.
         */
        get: function(request, callback) {
            if(request.length < 1) {
                return;
            }
            var mockVariant = request.length < mockData['long'][0].length ? 'short' : 'long';
            callback({items: mockData[mockVariant][1], metainfo: mockData[mockVariant][2]});
        },

        /**
         * Установка mock-данных
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
