var Susanin = require('susanin'),
    susanin = new Susanin();

susanin
    .addRoute({
        name: 'default',
        pattern: '/(<controller>/)',
        defaults: {
            controller: 'index'
        },
        data: {
            method: 'GET',
        }
    });

module.exports = susanin;
