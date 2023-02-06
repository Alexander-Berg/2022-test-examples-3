'use strict';

// Тег должен быть включен в разрешенный список.

const _ = require('lodash');
const j = require('jscodeshift');

const { invertDict } = require('../utils');
const { TEST_CALLEE_NAMES, YML_META_KEYS, YML_SPECS_TYPE_KEYS } = require('../constants');
const tagDict = require('./../dictionaries/tag-dictionary');

const KEY = YML_META_KEYS[0];
const tagDublicateDict = invertDict(tagDict);
const replace = value => _.uniq(
    [].concat(value)
        .map(tag => (_.isString(tag) && tagDublicateDict[tag]) || tag)
);

module.exports = {
    testpalm: data => {
        if (data[KEY]) {
            data[KEY] = replace(data[KEY]);
        }

        const traverse = node => {
            if (node && _.isArray(node)) {
                node
                    .forEach(step => {
                        const key = _.keys(step)[0];

                        if (_.isArray(step)) {
                            traverse(step);
                        } else if (key === KEY) {
                            step[key] = replace(step[key]);
                        }
                    });
            } else if (_.isObject(node)) {
                _.keys(node).forEach(key => traverse(node[key]));
            }
        };

        YML_SPECS_TYPE_KEYS.forEach(key => traverse(data[key]));

        return data;
    },
    hermione: ast => ast
};
