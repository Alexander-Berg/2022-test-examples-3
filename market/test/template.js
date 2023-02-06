import {metrika} from 'configs/current/node';
import {
    getStaticCssUrlByUserAgent,
    getStaticJsUrlByUserAgent,
    renderAsyncCounter,
} from 'src/main/server/helpers/static';

const entryName = 'widgets-playground/client/entries/index';

const template = ({cspNonce, user, abt}) => {
    const {userAgent} = user;

    const counterParams = {
        id: metrika.partnerWidgetsCounterId,
        clickmap: true,
        trackLinks: true,
        accurateTrackBounce: true,
    };

    return `
        <!DOCTYPE html>
        <html>
            <head>
                <title>Тестовая страница</title>

                <!-- Content Security Policy для JSS -->
                <meta property="csp-nonce" content="${cspNonce}">

                <link type="text/css" rel="stylesheet" href="${getStaticCssUrlByUserAgent(userAgent, entryName)}"/>
                <script type="text/javascript" nonce="${cspNonce}">
                    window.__INITIAL_DATA__ = {
                        metrikaCounterId: ${metrika.partnerWidgetsCounterId},
                        abt: ${JSON.stringify(abt)}
                    };
                </script>
                <script type="text/javascript" src="/widget/script/api"></script>
                <script type="text/javascript" src="${getStaticJsUrlByUserAgent(userAgent, entryName)}"></script>
                <style type="text/css" nonce="${cspNonce}">
                    html, body {
                        margin: 0;
                        padding: 0;
                    }
                </style>
            </head>

            <body>
                <div id="affiliateApp"></div>

                <!-- Yandex.Metrika counter -->
                ${renderAsyncCounter(cspNonce, counterParams)}
                <!-- /Yandex.Metrika counter -->
            </body>
        </html>
    `;
};

export default template;
