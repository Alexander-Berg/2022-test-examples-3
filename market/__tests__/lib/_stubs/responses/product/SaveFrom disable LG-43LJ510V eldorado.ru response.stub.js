/* eslint-disable max-len */

'use strict';

module.exports = {
    model: undefined,
    offers: undefined,
    searchResult: undefined,
    searchResultCaption: undefined,
    bucketInfo: {},
    shopInfo: undefined,
    notification: undefined,
    feedback: {
        constants: {
            closeButtonText: 'Закрыть',
            error: 'Комментарий не может быть пустым.',
            placeholder:
                'Пожалуйста, расскажите, что именно случилось. Ваш комментарий поможет нам исправить проблему быстрее.',
            submitButtonText: 'Отправить',
            text: 'Ваше сообщение поможет улучшить Советника',
            thanks: 'Спасибо!',
            title: 'Спасибо, мы получили ваш отчёт об ошибке',
        },
    },
    pricebar: {
        constants: {
            closeButtonTooltip: 'Закрыть',
            infoButtonTooltip: 'О программе',
            settingsButtonTooltip: 'Настройки',
        },
    },
    searchInfo: {
        originalQuery: 'LED телевизор LG 43LJ510V',
        filteredQuery: 'LED телевизор LG 43LJ510V',
        convertedPrice: {
            value: 25999,
            currencyCode: 'RUR',
        },
        offersCount: 0,
        urls: {
            shopsInfo: undefined,
            market: '<wrapped url>',
            search: undefined,
            searchButton: undefined,
            prices: undefined,
            eula: '<wrapped url>',
            feedback: '<wrapped url>',
            help: '<wrapped url>',
            helpPhone: '<wrapped url>',
            helpTablet: '<wrapped url>',
            disable: '<wrapped url>',
            features: '<wrapped url>',
            userHelp: '<wrapped url>',
        },
        category: undefined,
        categories: undefined,
        doNotComparePrice: undefined,
    },
    rules: undefined,
    optOutInfo: undefined,
    settings: {
        constants: {
            changeRegion: 'Изменить регион',
            changeSettings: 'Изменить настройки',
            disable: 'Выключить на этом сайте',
            yourRegion: 'Ваш регион:',
        },
        autoShowShopList: true,
        applicationName: 'SaveFrom',
        items: [
            {
                enabled: true,
                title: 'Показывать предложения из других регионов',
            },
            {
                enabled: true,
                title: 'Показывать список предложений при наведении мыши',
            },
        ],
        region: 'Москва',
        needShowNotifications: true,
        showProductNotifications: true,
        showAviaNotifications: true,
        showAutoNotifications: true,
    },
    doNotSearchReason: 'ya_bro_disabled',
    footer: {
        constants: {
            autoDetectedRegion: 'Автоматически',
            changeCurrentRegion: 'Изменить текущий регион',
            changeRegion: 'Изменить регион',
            data: {
                letter: 'Я',
                prefix: 'Данные',
                suffix: 'ндекс.Маркета',
            },
            infoAboutShop: 'Информация о продавцах',
            infoAboutShopLegal: 'Юридическая информация о продавцах',
            feedback: 'Сообщить об ошибке',
            gotoMarket: 'Перейти на Яндекс.Маркет',
            wrongProductDetect: 'Неверно определен товар',
        },
    },
    info: {
        constants: {
            disableText: 'Как отключить Советника',
            featuresText: 'Что ещё умеет Советник',
            feedbackText: 'Обратная связь',
            helpText: 'Помощь',
            licenseText: 'Лицензионное соглашение',
            prefix: 'для ',
            text: 'Это приложение подсказывает вам более выгодные цены на товары, на которые вы смотрите прямо сейчас.',
            upperLine: 'Яндекс.Советник',
            yandexLLC: '© %s ООО «Яндекс.Маркет»',
        },
    },
};
