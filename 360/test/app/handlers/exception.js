'use strict';

// eslint-disable-next-line no-unused-vars
module.exports = (req, res, next) => {
    setTimeout(() => decodeURIComponent('%EE'), 500);
    setTimeout(() => res.send('ok'), 4000);
};
