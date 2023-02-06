module.exports = function() {
    return {
        num: 0,
        construct: {
            baobab: {
                path: '/snippet/calories_fact'
            },
            data: {
                attributes: [
                    {
                        text_i18n: 'protein',
                        value: 0.9
                    },
                    {
                        text_i18n: 'fat',
                        value: 0.2
                    },
                    {
                        text_i18n: 'carbohydrate',
                        value: 8.1
                    }
                ],
                calories: '43',
                question: [
                    {
                        search_request: 'Апельсин',
                        text: 'Апельсин'
                    },
                    {
                        text_i18n: 'energy_value'
                    }
                ],
                size: [
                    {
                        text_i18n: 'calories_100g',
                        value: 100
                    },
                    {
                        text_i18n: 'entity_75',
                        value: 150
                    },
                    {
                        text_i18n: 'entity_65',
                        value: 100
                    }
                ]
            },
            headline: 'Калорийность Апельсин. Химический состав и пищевая ценность',
            host: 'health-diet.ru',
            norm_query: 'апельсин калория',
            path: {
                items: [
                    {
                        text: 'health-diet.ru',
                        url: 'http://health-diet.ru'
                    },
                    {
                        text: '27.php',
                        url: 'http://health-diet.ru/base_of_food/sostav/27.php'
                    }
                ]
            },
            ugc: {
                blocks: [
                    { questions: [{ type: 'radio', answers: [] }] }
                ],
                custom: { onlyFeedback: true }
            },
            type: 'calories_fact'
        }
    };
};
