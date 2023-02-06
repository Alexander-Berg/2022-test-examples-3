#!/usr/bin/env node
/* eslint-disable no-console */
'use strict';

const fs = require('fs');
const path = require('path');

const bodyParser = require('body-parser');
const express = require('express');

const httpPort = process.env.HTTP_PORT || 3000;

const staticLocation = process.env.USE_DIST ?
    path.join(__dirname, '..', '..', 'dist') :
    path.join(__dirname, '..', '..', 'src');

const app = express();
app.use(bodyParser.text({ type: '*/*' }));
app.use(express.static(staticLocation));
app.use(express.static(path.join(__dirname, '..', '..', 'test', 'assets')));

function getReqIdFromRequest(req) {
    if (req.query.reqid) {
        return req.query.reqid;
    }

    if (req.body) {
        const matches = /reqid=([\w.]+)[^\w.]?/.exec(req.body);
        if (matches) {
            return matches[1];
        }
    }

    return 'empty';
}

function parseCounters(counters) {
    counters = counters || [];

    return counters.map(item => {
        const chunks = item.split('/');

        return chunks.reduce((res, chunk) => {
            const matches = /(\w+)(?:=(.+))?/.exec(chunk);

            if (!matches) {
                return res;
            }

            const [name, val] = [matches[1], matches[2]];

            if (!name) {
                return res;
            }

            if (name === 'vars') {
                const varChunks = val.split(',');

                res[name] = varChunks.reduce((varRes, varData) => {
                    const [varName, varVal] = varData.split('=');
                    varRes[decodeURIComponent(varName)] = decodeURIComponent(varVal);
                    return varRes;
                }, {});
            } else {
                res[decodeURIComponent(name)] = decodeURIComponent(val);
            }

            return res;
        }, {});
    });
}

const counters = {};

app.get('/', (req, res) => {
    const reqId = getReqIdFromRequest(req);

    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.setHeader('Transfer-Encoding', 'chunked');

    fs.readFile(path.join(__dirname, 'test.html'), (err, content) => {
        if (err) {
            return res.status(500).end('Error: ' + err);
        }

        const viewContent = String(content);
        let [viewFirstChunk, viewSecondChunk] = viewContent.split('<!-- chunk-delimiter -->');

        // Отдаём чанками с тайм-аутом, чтобы успела произойти отрисовка
        res.write(viewFirstChunk.replace('%%reqId%%', reqId));

        // Для режима асинхронной загрузки удаляем тэги script из html, и загружаем общий бандл после load
        if (req.query.async_load) {
            viewSecondChunk = viewSecondChunk.replace(/<!-- script-section-start -->((.|\n)*)<!-- script-section-end -->/gm, _ => {
                return `<script>
                    window.addEventListener('load', function() {
                        var script = document.createElement('script');
                        script.src = '/bundle/all.js';
                        document.body.appendChild(script);
                    });
                    </script>`;
            });
        }

        setTimeout(() => {
            res.end(viewSecondChunk);
        }, 500);
    });
});

app.get('/ajax', (req, res) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.send(JSON.stringify({
        html: '<p>AJAX data</p>'
    }));
});

app.get('/ajax-json', (req, res) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.send(JSON.stringify({
        data: {
            ajax: 'json'
        }
    }));
});

app.get('/get-counters', (req, res) => {
    const reqId = getReqIdFromRequest(req);
    const data = parseCounters(counters[reqId]);

    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.send(JSON.stringify(data, null, 2));
});

app.post('/clck', (req, res) => {
    const reqId = getReqIdFromRequest(req);

    if (!counters[reqId]) {
        counters[reqId] = [];
    }

    const data = (req.body || '').trim();
    const lines = data.split('\n');

    for (const line of lines) {
        const counterData = line.trim();
        counters[reqId].push(counterData);
        console.log(`${reqId}: Received counter ${counterData}`);
    }

    res.send('OK');
});

app.get('/bundle/all.js', (req, res) => {
    // При асинхронной вставке библиотеки нужно склеить файлы в бандл, чтобы порядок выполнения был гарантированный.
    // Иначе появляются ошибки, когда один файл выполнился раньше другого.
    const bundle = [
        'send.js',
        'implementation.js',
        'ajax.js',
        'onload.js',
        'image-goodness.js',
        'scroll.js'
    ].reduce((acc, file) => {
        const p = path.join(__dirname, '..', '..', process.env.USE_DIST ? 'dist' : 'src', 'bundle', file);

        return acc + fs.readFileSync(p);
    }, '');

    res.setHeader('Content-Type', 'application/javascript; charset=utf-8');
    res.send(bundle);
});

app.listen(httpPort);
console.log(`Test server started on port ${httpPort}`);
