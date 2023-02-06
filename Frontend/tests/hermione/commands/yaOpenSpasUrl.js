// eslint-disable no-console
'use strict';
const SPAS_SCRIPT_SRC = process.env.SPAS_SCRIPT_SRC || 'https://frontend-test.s3.mds.yandex.net/search-spa/2208341/9b449467ff731a0fba8f25d87851e0fb9db3237b/01b5b6e62c1ca5c395a24415c350e54864f2e64d/static/init.js';

async function openSPASFromWeb4(originalUrl) {
    await this.execute(() => {
        (function(window, prevOnErrorHandler) {
            'use strict';

            let jsErrors = window['hermione-jserrors'] = [];

            function pushError(message, url, line, col, err) {
                jsErrors.push({
                    message: message,
                    url: url,
                    line: line,
                    col: col,
                    error: err,
                });
            }

            window.onerror = function(message, url, line, col, err) {
                pushError(message, url, line, col, err);
                return prevOnErrorHandler && prevOnErrorHandler(message, url, line, col, err);
            };
        })(window, window.onerror);
    });

    // Открываем оригинал, чтобы получить полный абсолютный урл.
    await this.yaOpenPageByUrl(originalUrl);
    const testUrl = new URL(await this.getUrl());

    // const web4UrlPrefix = hermione.ctx.platform === 'desktop' ? 'search' : 'search/touch';
    const web4UrlPrefix = 'search';
    const web4Url = new URL(`/${web4UrlPrefix}/`, testUrl.origin);
    web4Url.searchParams.set('exp_flags', 'spas=1;spas_whitelist=goods');
    web4Url.searchParams.set('ajax-test', testUrl.toString());
    web4Url.searchParams.set('text', 'test');

    if (SPAS_SCRIPT_SRC) {
        web4Url.searchParams.append('exp_flags', `spas_script_src=${SPAS_SCRIPT_SRC}`);
    }

    await this.url(web4Url.toString());

    await this.waitUntil(async() => {
        return await this.execute(function() {
            return Boolean(window.Ya && window.Ya.SearchSPA);
        });
    });

    // eslint-disable-next-line no-console
    console.log('goSPASUrl открывает URL: ', testUrl.toString());

    await this.execute(function(url) {
        let p = window.Ya.SearchSPA.go(url);

        if (p && p.catch) {
            p.catch(error => alert(error));
        }
    }, [testUrl.toString()]);

    await this.execute(function() {
        setTimeout(() => {
            if (window['hermione-jserrors'] && window['hermione-jserrors'].length) {
                alert(JSON.stringify(window['hermione-jserrors'], null, 4));
            }
        }, 5000);
    });

    await this.waitUntil(async() => {
        const status = await this.execute(function() {
            // eslint-disable-next-line no-var
            var spas = window.Ya && window.Ya.SearchSPA;

            return spas && spas.getCurrentAppStatus();
        });

        return status === 'active';
    }, { timeout: 20000 });
}

/**
 * Имитирует открытие урла SPASearchCore
 *
 * @param {Object} opts - объект в формате для либы urijs
 * @returns {Webdriverio}
 */
async function yaOpenSpasUrl(url) {
    await openSPASFromWeb4.call(this, url);
}

module.exports = yaOpenSpasUrl;
