function step121Middleware(req, res, next) {
    req.data.push('1-2-1');
    res.data.push('1-2-1');

    next();
}

function step122Middleare(req, res, next) {
    req.data.push('1-2-2');
    res.data.push('1-2-2');

    next();
}

module.exports = [
    step121Middleware,
    step122Middleare
];