const fs = require('fs');
const path = require('path');


const TOUCH = '../skipped/market/touch.js';
const TOUCH_JSON = `${TOUCH}on`;

const touch = require(TOUCH);

fs.writeFileSync(
    path.resolve(TOUCH_JSON),
    JSON.stringify(touch, null, 4)
);

const DESKTOP = '../skipped/market/desktop.js';
const DESKTOP_JSON = `${DESKTOP}on`;

const desktop = require(DESKTOP);

fs.writeFileSync(
    path.resolve(DESKTOP_JSON),
    JSON.stringify(desktop, null, 4)
);

