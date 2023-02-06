module.exports = function() {
    return {
        meta: {
            type: 'gray',
            items: [[
                {

                     "block": "rating2",
                     "value": 5.9,
                     "base": 10,
                     "size": "s",
                     "type": "extended"

                },
                {
                    "block": "i18n",
                    "keyset": "adapter-snip-rating",
                    "key": "{count} оценка",
                    "some": "{count} оценки",
                    "many": "{count} оценок",
                    "context": "Количество оценок, на основании которых рассчитывался рейтинг",
                    "count": "7115"
                }
            ]]
        }
    };
};
