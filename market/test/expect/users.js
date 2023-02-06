const DEFAULT = 'DEFAULT_USER';

const users = {
    // Обычный юзер-москвич
    DEFAULT_USER: {
        region: {
            info: {
                id: 213,
                country: 225,
            },
        },
        tld: 'ru',
    },
    // Питербуржец
    SPB_USER: {
        region: {
            info: {
                id: 2,
                country: 225,
            },
        },
        tld: 'ru',
    },
};

module.exports = function (type) {
    return users[type] || users[DEFAULT];
};
