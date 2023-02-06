const BUILD_WIDGET = 'widget';
const BUILD_YANDEX = 'widget_ya';
const BUILD_YATEAM = 'widget_ya_internal';

const defaultParams = {
    autocloseable: false,
    badgeCount: 0,
    badgeMaxCount: 99,
    badgeType: 'dot',
    build: 'widget',
    buttonIcon: '',
    buttonText: 'Напишите нам!',
    config: '',
    collapsedDesktop: 'never',
    collapsedTouch: 'never',
    events: [],
    iframeUrl: '',
    iframeUrlParams: {},
    isMobile: false,
    lang: 'ru',
    guid: '',
    showChatParams: '{ guid: \'\' }',
    title: 'Текст в шапке чата',
    theme: 'light',
    unreadDisabled: true,
};

const getUrl = (build, params = {}) => {
    return `/chat?build_script=${build}&hermioneParams=${JSON.stringify({
        ...defaultParams,
        ...params,
    })}`;
};

const widgetParamsUpdaterFactory = (params = {}) => {
    return [
        (stringifiedParams) => {
            window._updateWidget(JSON.parse(stringifiedParams));
        },
        JSON.stringify({ ...defaultParams, ...params }),
    ];
};

module.exports = {
    defaultParams,
    getUrl,
    build: {
        widget: BUILD_WIDGET,
        yandex: BUILD_YANDEX,
        yateam: BUILD_YATEAM,
    },
    widgetParamsUpdaterFactory,
};
