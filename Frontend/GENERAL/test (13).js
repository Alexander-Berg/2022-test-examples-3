import http from 'http';
import express from 'express';
import test from 'ava';
import hostFallbackMiddleware from '.';

const PORT = 3000;

function runApp(middlewareOptions, testMiddleware) {
    const app = express();

    app.use([
        hostFallbackMiddleware(middlewareOptions),
        testMiddleware,
    ]);

    app.use((req, res) => {
        res.send('ok');
    });

    const server = http.createServer(app);
    server.listen(PORT);

    return server;
}

function request(opts) {
    return new Promise(resolve => {
        http.get({
            hostname: 'localhost',
            port: PORT,
            path: '/',
            agent: false,
            headers: {
                Host: opts.host,
            },
        }, resolve);
    });
}

test.cb('should fallback host', t => {
    const fallBackHost = 'test.ru';

    const testMiddleware = (req, res, next) => {
        t.is(req.headers.host, fallBackHost);
        t.is(req.hostname, fallBackHost);
        t.is(req.get('Host'), fallBackHost);

        next();
    };

    const testApp = runApp({ host: fallBackHost }, testMiddleware);

    request({ host: ' ' }).then(() => {
        testApp.close();
        t.end();
    });
});

test.cb('shouldn\'t fallback host if host is correct', t => {
    const correctHost = 'correct.ru';

    const testMiddleware = (req, res, next) => {
        t.is(req.headers.host, correctHost);
        t.is(req.hostname, correctHost);
        t.is(req.get('Host'), correctHost);

        next();
    };

    const testApp = runApp({ host: 'test.ru' }, testMiddleware);

    request({ host: correctHost }).then(() => {
        testApp.close();
        t.end();
    });
});
