(function() {

    // todo@dima117a нужен рефакторинг https://st.yandex-team.ru/DIRECT-65106

    u.register({
        /**
         * Плюрализация
         * @param {Array} forms формы слова [1, 2, 5]
         * @param {Number} count количество
         * @returns {String}
         */
        pluralizeWord: function(forms, count) {

            var lang = u.consts('lang') || 'ru';

            if (lang == 'ru' || lang == 'ua') {
                count %= 100;
                if (count >= 5 && count <= 20) {
                    return forms[2];
                }
                count %= 10;
                if (count == 1) {
                    return forms[0];
                } else if (count < 5 && count > 0) {
                    return forms[1];
                }
                return forms[2];
            } else if (lang == 'en' || lang == 'tr') {
                if (count == 1) return forms[0];
                return forms[1];
            }
            throw 'unsupported lang';
        },

        /**
         * Формирование строки для отображения количества элементов
         * @param {Array} forms формы слова [1, 2, 5]
         * @param {Number} count количество
         * @param {String} delimiter разделитель
         * @returns {String}
         * @example
         *
         * u.pluralize(['яблоко', 'яблока', 'яблок'], 5)
         * // => '5 яблок'
         */
        pluralize: function(forms, count, delimiter) {
            return count + (delimiter || ' ') + u.pluralizeWord(forms, count);
        },

        /**
         * Подстановка в строку значений в нужной форме
         * @param {String} str шаблон строки
         * @returns {String}
         * @example
         *
         * u.pluralForms('Буратине дали ' + x + ' {яблоко|яблока|яблок}', x)
         * x == 3 => 'Буратине дали 3 яблока'
         */
        pluralForms: function(str) {
            var args = Array.prototype.slice.call(arguments, 1);

            return str.replace(/{([^}]+)}/g, function(s, $1) {
                return u.pluralizeWord($1.split('|'), args.shift());
            });
        }
    });
})();
