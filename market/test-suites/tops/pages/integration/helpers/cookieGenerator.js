const generateYandexuidValue = function () {
    return (Math.floor(Math.random() * ((10 ** 9) - (10 ** 7))) + (10 ** 7)).toString() +
        Math.floor(new Date().getTime() / 1000);
};

export default {
    generateYandexUidCookie: () => ({
        name: 'yandexuid',
        value: generateYandexuidValue(),
        domain: '.yandex.ru',
        secure: false,
        httpOnly: false,
    }),

    generateBackendCookie: () => ({
        name: 'backend',
        value: process.env.BACKEND_HOST || 'msh02et.market.yandex.net:17051',
    }),
};
