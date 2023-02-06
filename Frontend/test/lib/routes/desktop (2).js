var Susanin = require('susanin'),
    susanin = new Susanin();

susanin
    .addRoute({
        name : 'catalog',
        pattern : '/<mark>/<model>/<cid>(/)'
    });

module.exports = susanin;
