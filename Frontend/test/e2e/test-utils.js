/* eslint-disable no-console, dot-notation, @typescript-eslint/no-use-before-define */
'use strict';
const { spawn } = require('child_process');
const path = require('path');

const puppeteer = require('puppeteer');

const { assert } = require('chai');
const fetch = require('node-fetch');

let useServer = Number(process.env.USE_SERVER);
if (isNaN(useServer)) {
    useServer = 1;
}
const passLogs = Number(process.env.PASS_SERVER_LOGS);

const httpPort = process.env.HTTP_PORT || 3000;
const serverScript = path.join(__dirname, 'server.js');

const timeout = 30000;

module.exports.describeRum = cb => describe('RUM', function() {
    this.timeout(timeout);

    before(async function() {
        this.browser = await puppeteer.launch({
            headless: true,
            devtools: false,
            slowMo: 0,
            defaultViewport: {
                width: 1024,
                height: 2048,
                deviceScaleFactor: 1,
                isMobile: false,
                hasTouch: false,
                isLandscape: false
            },
            timeout
        });

        if (useServer) {
            this.server = spawn(serverScript, {
                env: Object.assign({}, process.env, { HTTP_PORT: httpPort })
            });

            if (passLogs) {
                this.server.stdout.on('data', data => {
                    console.log(String(data));
                });

                this.server.stderr.on('data', data => {
                    console.error(String(data));
                });
            }

            // Ожидаем поднятия сервера
            await new Promise(resolve => {
                setTimeout(resolve, 1500);
            });
        }
    });

    after(async function() {
        this.browser.close();

        if (this.server) {
            this.server.kill();

            // Ждём завершения сервера
            await new Promise(resolve => {
                setTimeout(resolve, 500);
            });
        }
    });

    cb(httpPort);
});

module.exports.getCountersByPath = function(counters, path) {
    return counters.filter(x => x.path === path);
};

module.exports.checkCounter = function(counter, requiredTimings) {
    assert.equal(counter.slots, '1;2', 'Incorrect slots');

    assert.equal(counter.vars['143'], '28.1786', 'Page is incorrect');
    assert.equal(counter.vars['287'], '213', 'Region is incorrect');
    assert.equal(counter.vars['2923'], '1', 'There is no ad block sign');
    assert.equal(counter.vars['-custom-var'], '/test', 'Custom variable is incorrect');

    assert.ok(counter.vars['1042'], 'User-Agent is invalid');

    for (const name of requiredTimings) {
        const val = parseFloat(counter.vars[name]);

        assert.isNotNaN(val, `${name} is NaN`);
        assert.isAtLeast(val, 0, `${name} is < 0`);
        assert.isBelow(val, 30000, `${name} is >= 30000`);
    }
};

module.exports.getCounters = async function(reqId, path) {
    let response;
    for (let i = 0; i < 3; i++) {
        try {
            response = await fetch(`http://localhost:${httpPort}/get-counters?reqid=${reqId}`);
            break;
        } catch (e) {
            console.warn('WARN: Retry for get counters');
        }
    }

    const data = await response.json();

    if (!path) {
        return data;
    }

    return [].concat(data).filter(counter => counter.path === path);
};
