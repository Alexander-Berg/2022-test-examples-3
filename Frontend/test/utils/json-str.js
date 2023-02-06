const jsonStableStringify = require('json-stable-stringify');

const maxPriorKeys = new Set([
    'group',
    'markup',
    'type',
]);

const bigPriorKeys = new Set([
    'offset',
    'title',
    'start',
]);

const minPriorKeys = new Set([
    'children',
]);

const jsonOpts = {
    space: 4,
    cmp(a, b) {
        if (maxPriorKeys.has(a.key)) {
            return -1;
        }

        if (maxPriorKeys.has(b.key)) {
            return 1;
        }

        if (bigPriorKeys.has(a.key) && !bigPriorKeys.has(b.key)) {
            return -1;
        }

        if (bigPriorKeys.has(b.key) && !bigPriorKeys.has(a.key)) {
            return 1;
        }

        if (minPriorKeys.has(a.key)) {
            return 1;
        }

        if (minPriorKeys.has(b.key)) {
            return -1;
        }

        if (a.key > b.key) {
            return 1;
        }

        if (b.key > a.key) {
            return -1;
        }

        return 0;
    },
};

function jsonStr(node) {
    return jsonStableStringify(node, jsonOpts);
}

exports.jsonStr = jsonStr;
