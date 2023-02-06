/* eslint-disable import/no-extraneous-dependencies */

let Susanin = require('susanin');
let susanin = new Susanin();

susanin
    .addRoute({
        name: 'default',
        pattern: '/(<controller>/)',
        defaults: {
            controller: 'index',
        },
        data: {
            method: 'GET',
        },
    });

module.exports = susanin;
