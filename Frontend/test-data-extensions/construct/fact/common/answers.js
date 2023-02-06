module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            type: 'test',
            template: 'composite',
            items: [
                {
                    block: 'fact',
                    question: ['Размер ответа', 'XXL'],
                    answer: 'Справка'
                },
                {
                    block: 'fact',
                    question: ['Размер ответа', 'XL'],
                    answer: 'Фактовый ответ'
                },
                {
                    block: 'fact',
                    question: ['Размер ответа', 'L'],
                    answer: 'Представление справочного ответа'
                },
                {
                    block: 'fact',
                    question: ['Размер ответа', 'M'],
                    answer: 'Подходит для представления различных фактов об объектах'
                },
                {
                    block: 'fact',
                    question: ['Размер ответа', 'S'],
                    answer:
                    'Медийное представление справочного ответа.' +
                    'Подходит для представления различных фактов об объектах, определений, подсказок.'
                }
            ]
        }
    }
};
