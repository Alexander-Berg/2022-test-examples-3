const fs = require('fs');
const express = require('express');
const app = express();
const {template} = require('./templates');

const STATIC_ADDRESS = 'yastatic.net/s3/webmaster/siteform'; // TODO: поменять на 'site.yandex.net' после настройки проксирования

app.get('/ping', (req, res) => res.send('pong'));

app.use((req, res, next) => {
    const path_chunks = req.path.split('/').filter(Boolean);

    if (path_chunks.length < 2) {
        res.status(404);
        res.send(`${req.path} not found`)
        return;
    }

    const templateName = path_chunks.pop();
    const serp = `//${ path_chunks.join('/') }`;

    const staticUrl = `${STATIC_ADDRESS}${process.env.PR_NUMBER ? '/pr/' + process.env.PR_NUMBER : ''}`;

    template(templateName, { serp, staticUrl }, (err, html) => {
        if (err) {
            res.send(JSON.stringify(err));
            res.status(err.code);
            return;
        }

        res.send(html)
    });
});

app.listen(process.env.NODE_PORT || 80, () => console.log('App listening on port 3000!'));