const _ = require('lodash');
const config = require('../config');
const layout = require('../util/layout');

const METHODS = [
    'GET', 'POST', 'PATCH', 'PUT', 'DELETE'
];

module.exports = (req, res, next) => {
    let accessAvailable = (
        /^(development|.*\.test)$/.test(config.env) ||
        _.get(req.context, 'session.roles', []).indexOf('admin') !== -1
    );

    if (!accessAvailable) {
        res.status(404);
        return next();
    }

    let userTicket = _.get(req.context, 'headers.X-Ya-User-Ticket');

    layout(req, res).render('test_playground', {
        METHODS,
        api: _.omit(config.api, ['blackbox', 'tvm', 'directory']),
        headers: {
            'Content-Type': 'application/json',
            'x-user-ip': '127.0.0.1',
            'X-Ya-User-Ticket': userTicket,
            'X-Ya-Service-Ticket': _.get(req.context, 'headers.X-Ya-Service-Ticket')
        },
        user_ticket: userTicket,
        tvm_tickets: req.context.tvm_tickets,
        scripts: ['test_playground.js']
    });
};
