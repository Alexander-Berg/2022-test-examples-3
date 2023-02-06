'use strict';

// spec_13: Все -do: -assert должны быть с маленькой буквы

const _ = require('lodash');

const { YML_SPECS_TYPE_KEYS, YML_STEP_KEYS } = require('./../constants');
const STEP_KEYS = YML_STEP_KEYS.slice(0, 2);

const replace = value => _.lowerFirst(value);

module.exports = {
    testpalm: data => {
        const traverse = node => {
            if (node && _.isArray(node)) {
                node
                    .filter(step => STEP_KEYS.includes(Object.keys(step)[0]) || _.isArray(step))
                    .forEach(step => {
                        if (_.isArray(step)) {
                            traverse(step);
                        } else {
                            const key = Object.keys(step)[0];

                            step[key] = replace(step[key]);
                        }
                    });
            } else if (_.isObject(node)) {
                Object.keys(node).forEach(key => traverse(node[key]));
            }
        };

        YML_SPECS_TYPE_KEYS.forEach(key => traverse(data[key]));

        return data;
    },
    hermione: ast => ast
};
