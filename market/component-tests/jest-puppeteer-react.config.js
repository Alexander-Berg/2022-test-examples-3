const isWsl = require('is-wsl');

const isMac = process.platform === 'darwin';
const isWin32 = process.platform === 'win32';

const useDocker =
    process.env.CI !== '1' && process.env.SCREENSHOT_WITHOUT_DOCKER !== '1' && process.env.SCREENSHOT_DEBUG !== '1';

const dockerHost = () => {
    if (isMac) {
        return 'docker.for.mac.host.internal';
    }
    if (isWin32 || isWsl) {
        return 'host.docker.internal';
    }
    return '172.17.0.1';
};

const config = {
    port: 1111,
    renderOptions: {
        viewport: {deviceScaleFactor: 1},
        // вот этот флаг можно поменять, чтобы посмотреть логи из консоли браузера
        dumpConsole: false,
        before: async page => {
            await page.setDefaultNavigationTimeout(0);
            // Мокаем роуты для puppeteer
            await page.evaluateOnNewDocument(() => {
                // eslint-disable-next-line no-undef
                window.state = {
                    routes: {
                        root: {url: '/'},
                    },
                };
            });
            page.on('load', () => {
                const content = `
                *,
                *::after,
                *::before {
                    transition: none !important;
                    transition-delay: 0s !important;
                    transition-duration: 0s !important;
                    animation-delay: -0.0001s !important;
                    animation-duration: 0s !important;
                    animation-play-state: paused !important;
                    caret-color: transparent !important;
                    font-smooth: never;
                    -webkit-transition: none !important;
                    -webkit-font-smoothing: none;
                }`;

                page.addStyleTag({content});
            });
        },
    },
    useDocker,
    dockerHost: dockerHost(),
    dockerImageName: 'registry.yandex.net/market/levitan-jest-puppeteer-react',
    dockerRunOptions: `--shm-size=${
        process.env.SCREENSHOT_UPDATE_ALL === '1' || process.env.SCREENSHOT_I18N === '1' ? 16 : 8
    }gb -e TZ=${process.env.TZ || 'Europe/Moscow'}`,
};

module.exports = {
    config,
};
