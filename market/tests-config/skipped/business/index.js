import {readFileSync} from 'fs';
import {resolve} from 'path';

const desktop = JSON.parse(
    readFileSync(resolve(__dirname, './desktop.json')).toString()
);
const touch = JSON.parse(
    readFileSync(resolve(__dirname, './touch.json')).toString()
);

module.exports = {
    desktop,
    touch,
};
