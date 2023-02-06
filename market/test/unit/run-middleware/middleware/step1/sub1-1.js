function step11Middleware(req, res, next) {
    req.data = ['1-1'];
    res.data = ['1-1'];
    
    next();
}

module.exports = step11Middleware;