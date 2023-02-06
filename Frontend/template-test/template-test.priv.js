(function() {
   /**
    * Тестовый шаблон всегда использует только 0-ой элемент doc.construct
    *
    * Тестовый шаблон поддерживает 2 формата записаны данных:
    * 1. Все блоки указаны в массиве blocks,
    * имя блока лежит в свойстве 'block',
    * данные лежат в соседних для 'block' полях.
    * Если в конструкторе нет блока с указанным именем, то тестовый шаблон считает такие данные готовым bemjson'ом и
    * отправит их на шаблонизацию
    *
    * Нормализованные данные
    * @example
    * doc.construct = {
    *     type: 'test', // используем тестовый адаптер
    *     template: 'test', // используем тестовый шаблон
    *     blocks: [{
    *         block: 'fact',
    *         question: ['Размер ответа', 'XXL'],
    *         answer: 'Справка'
    *     }, {
    *         block: 'image', // такого блока в конструкторе нет, поэтому данные будут защитаны bemjson'ом
    *         url: 'https://mdata.yandex.net/i?path=b0910230234_img_id2130334858748450706.jpg'
    *     }]
    * }
    *
    * 2. Блоки указаны в виде пар ключ(имя блока)-значение(данные для блока)
    *
    * Денормализованные данные
    * @example
    * construct: {
    *     type: 'test',
    *     template: 'test',
    *     fact: {
    *         question: ['Размер ответа', 'XXL'],
    *         answer: 'Справка'
    *     }
    * }
    * @param {Object} datasets
    * @returns {Bemjson}
    */
    blocks['template-test'] = function(datasets) {
        // Берем 0-ой элемент doc.construct
        var context = datasets[0].context,
            dataset = datasets[0].data;

        if (!context.isYandexNet) {
            throw new Error('Template "test" is not allowed outside yandex network.');
        }

        return {
            block: 'serp-item',
            content: instantiateBlocks(context, _.isArray(dataset.blocks) ? dataset : normalizeData(dataset))
        };

        /**
         * Нормализует данные (про форматы данных см. документацию блока template-default)
         * @param {Object} denormalizedData
         * @returns {NormalizedData}
         */
        function normalizeData(denormalizedData) {
            // свойства, которые точно не могут быть именами блоков
            var reservedProps = ['template', 'block', 'type', 'adapter', 'counter'];

            // приводим данные к стандартному формату с массивом в поле `blocks`
            return _.assign(
                _.pick(denormalizedData, reservedProps),
                {
                    blocks: _(denormalizedData)
                        .omit(reservedProps)
                        .mapValues(function(object, name) {
                            return _.assign(object, { block: name });
                        })
                        .values()
                        .value()
                }
            );
        }

        /**
         * Создает блок для каждого элемента данных
         * @param {Context} context
         * @param {NormalizedData} normalizedData
         * @returns {Bemjson}
         */
        function instantiateBlocks(context, normalizedData) {
            return normalizedData.blocks.map(function(blockData) {
                return {
                    block: 'serp-item',
                    elem: 'test-block',
                    content: _.isFunction(blocks[blockData.block]) ?
                        invokeBlock(context, blockData) :
                        {
                            block: 'error-message',
                            content: reportError(
                                'В конструкторе не существует блока blocks[\'' + blockData.block + '\']',
                                JSON.stringify(blockData, null, '\t')
                            )
                        }
                };
            });
        }

        /**
         * Вызывает блок конструктора с правильными параметрами
         * @param {Context} context
         * @param {Object} dataset
         * @returns {Bemjson}
         */
        function invokeBlock(context, dataset) {
            return dataset.block === 'organic' ?
                blocks['organic']([{ context: context, data: dataset }]) :
                blocks['construct__block'](context, dataset);
        }

        /**
         * @param {String} header
         * @param {String} content
         *
         * @returns {Bemjson[]} блок ошибки
         */
        function reportError(header, content) {
            return [
                {
                    attrs: {
                        style: 'border: solid red;' +
                        'border-width: 1px 1px 0 1px;' +
                        'background: #fee;' +
                        'padding: 5px;'
                    },
                    content: [
                        { attrs: { style: 'font-weight: bold' }, content: header },
                        {
                            attrs: { style: 'font-size: 70%' },
                            content: 'Без паники! Это сообщение видно только во ' +
                                     'внутренней сети Яндекса и привязанным логинам.'
                        }
                    ]
                },
                {
                    tag: 'pre',
                    attrs: {
                        style: 'border: 1px solid red;' +
                        'padding: 5px;' +
                        'margin: 0;' +
                        'font-size: 70%;' +
                        'overflow: scroll;'
                    },
                    content: Text.xmlEscape(content)
                }
            ];
        }

        /**
         * @typedef {Object} NormalizedData
         * @property {Array.<Object>} blocks
         */
    };
})();

/**
 * Шаблон для отрисовки произвольных блоков. Использовать можно только во внутренней сети.
 *
 * @typedef {Object} TemplateTest
 *
 * @property {Block[]} blocks - Массив блоков, которые будут отрисованы один над другим.
 */
