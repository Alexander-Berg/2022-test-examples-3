const path = require('path');

const express = require('express');
const bodyParser = require('body-parser');
const marketLogger = require('@yandex-market/logger');

const {makeRenderApp} = require('analytics/extensions/analyticsRendering');
const {getRemoteResolver} = require('analytics/resolvers/getAnalyticsResolver');
const {getBackendHandler, setBackendHandler, removeBackendHandler} = require('isomorphic/testingMocks/backendHandlers');

// eslint-disable-next-line no-undef
const {port} = require('./config');
const logger = require('./logger');

const app = express();

const host = null; // Let http.Server use its default IPv6/4 host
const prettyHost = 'localhost';
const render = makeRenderApp();

module.exports = (() => {
    let server;
    const stateMap = {};
    const startServer = () => {
        marketLogger.setup(logger.log, 'trace');
        app.use(bodyParser());
        app.use('/s3/market-static/partnernode', express.static(path.resolve(process.cwd(), 'freeze/static')));

        app.get('/welcome', (req, res) => {
            res.send('promo page');
        });

        app.get('*', (req, res) => {
            const sk = req.header('sk');
            render({initialState: stateMap[sk], path: '/', ctx: {sk}})
                .then(result => {
                    stateMap[sk] = undefined;
                    res.send(result);
                })
                .catch(error => {
                    const code = error?.CODE;
                    switch (code) {
                        case 'ISOMORPHIC_PAGE_REDIRECT':
                            res.redirect(error.newUrl);
                            break;
                        case 'ISOMORPHIC_PAGE_NOT_FOUND':
                            res.sendStatus(404);
                            break;
                        default:
                            res.sendStatus(500);
                            break;
                    }
                });
        });

        app.post('/api/gateway/', (req, res) => {
            const {body, query} = req;
            const sk = req.header('sk');
            Promise.all(
                [].concat(query.r).map((rValue, index) => {
                    const [namespace, name] = rValue.split(':');
                    const params = body.params[index];
                    const resolver = getRemoteResolver(namespace, name);

                    return resolver({sk}, params);
                }),
            )
                .then(results => ({
                    results: results.map(data => ({data})),
                }))
                .then(result => {
                    res.send(result);
                });
        });

        server = app.listen(port, host, err => {
            if (err) {
                logger.error(err.message);
            } else {
                logger.appStarted(port, prettyHost);
            }
        });
    };
    const stopServer = () => {
        if (server) {
            server.close();
        }
    };

    return {
        startServer,
        stopServer,
        getBackendHandler,
        setBackendHandler,
        removeBackendHandler,
        getHostName: () => `http://${prettyHost}:${port}`,
        setInitialState: state => {
            stateMap[state.widgets.currentUser.sk] = state;
        },
    };
})();
