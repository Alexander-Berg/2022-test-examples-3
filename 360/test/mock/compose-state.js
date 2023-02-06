before(function() {

    var DEFAULT_DATA = {
        /**
         * Высота поля ввода текста письма
         */
        messageHeight: 150,
        /**
         * Текущая позиция курсора в plain режиме
         */
        bodyPos: 0,

        /**
         * Показано ли поле cc
         */
        isCcVisible: false,

        /**
         * Показано ли поле bcc
         */
        isBccVisible: false,

        /**
         * Текст кнопки submit
         */
        submitButtonText: i18n('%Отправить')
    };

    window.mock['compose-state'] = [
        {
            params: {},
            data: DEFAULT_DATA
        },

        {
            params: {
                ids: '123',
                oper: 'reply'
            },
            data: DEFAULT_DATA
        }
    ];

});
