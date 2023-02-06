/**
 * https://wiki.yandex-team.ru/quasarui/testing-speakers/
 */

const express = require('express');
const expressTvmMiddleware = require('@yandex-int/express-tvm').default;
const axios = require('axios');
const { exec } = require('child_process');

const app = express();
const port = 8002;
const tvmport = 8011;

exec(`tvmtool --port ${tvmport} --auth tvmtool-development-access-token`);

app.use(expressTvmMiddleware({
    // clientId мы определили в .tvm.json
    clientId: 'quasarui-hermione',
    // Допустимые destinations для clientId мы определили в .tvm.json
    destinations: ['quasar-backend'],
    // Server URL HTTP API локально запущеного TVM-демона (передавали через --port)
    serverUrl: 'http://localhost:' + tvmport,
    // Токен для подключения (передавали через --token)
    token: 'tvmtool-development-access-token',
}));

app.use(express.json());

app.use((req, res, next) => {
    if (req.path === '/quasarui-hermione') {
        if (!req.tvm || !req.tvm['quasarui-hermione']) {
            throw new Error('TVM не отработал');
        }
        if (req.tvm['quasarui-hermione'].error) {
            throw new Error(req.tvm['quasarui-hermione'].error.message);
        }
        if (req.url.indexOf('?') === -1) {
            throw new Error('Нету пути запроса (отделяется "?")');
        }

        const ticket = req.tvm['quasarui-hermione'].tickets['quasar-backend'].ticket;
        let path = req.url.substring(req.url.indexOf('?') + 1);
        if (path.indexOf('/') === -1) {
            path = decodeURIComponent(path);
        }
        path = 'https://testing.quasar.yandex.ru' + path;

        const method = req.method;

        process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
        axios({
            method,
            url: path,
            data: req.body,
            headers: {
                'X-Ya-Service-Ticket': ticket,
            },
        }).then(resp => {
            process.env.NODE_TLS_REJECT_UNAUTHORIZED = '1';
            res.status(200).send(resp.data);
        }).catch(resp => {
            process.env.NODE_TLS_REJECT_UNAUTHORIZED = '1';
            console.error(resp.data);
            res.status(500).send(resp.data);
        });
    } else {
        throw new Error('Неизвестный урл ' + req.path);
    }
});

app.listen(port);
