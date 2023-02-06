const express = require('express');
const http = require('http');
const cookieParser = require('cookie-parser');

const graphiteMiddleware = require('./middleware/graphite');
const logsMiddleware = require('./middleware/logs');
const disableMiddleware = require('./middleware/disable');
const dataExtractionMiddleware = require('./middleware/data-extraction');
const promoMiddleware = require('./middleware/promo');
const referralMiddleware = require('./middleware/referral');
const trackReferralDataMiddleware = require('./middleware/track-referral-data');
const statsMiddleware = require('./middleware/stats-middleware');
const responseMiddleware = require('./middleware/response-middleware');

const { noticeError } = require('./utils/winston-logs');

const app = express();

// all environments
app.set('port', process.env.port || 8081);
app.use(cookieParser('lJLJewojr3289434jlJfdsklnklsfjr42389434894923jsdfzzzj324'));

app.get(
    '/redir',
    [
        graphiteMiddleware,
        disableMiddleware,
        logsMiddleware,
        dataExtractionMiddleware,
        promoMiddleware,
        referralMiddleware,
        trackReferralDataMiddleware,
        statsMiddleware,
        responseMiddleware,
    ],
    errorHandler,
);

app.get('/ping', (req, res) => res.send('0;OK'));

app.get('/ping-balancer', (req, res) => res.send('0;OK'));

const server = http.createServer(app).listen(app.get('port'), () => {
    console.log(`Express server listening on port ${app.get('port')}`);
});

function errorHandler(err, req, res, next) {
    noticeError(err, req);
}
