'use strict';

// spec_7: Название тест-кейса должно начинаться с большой буквы. Исключая случаи со специальными словами

const _ = require('lodash');
const j = require('jscodeshift');

const { TEST_CALLEE_NAMES, YML_SPECS_TYPE_KEYS, YML_IGNORE_KEYS } = require('./../constants');
const specialWordDict = require('./../dictionaries/special-word-dictionary');
const { astReplace } = require('./../utils');

const CALLEE_NAMES = TEST_CALLEE_NAMES.slice(1);

const replace = value => {
    if (specialWordDict.some(word => String(value) === word || String(value).startsWith(`${word} `))) {
        return value;
    }

    return _.upperFirst(value);
};

module.exports = {
    testpalm: data => {
        const traverse = node => {
            if (node && _.isObject(node) && !_.isArray(node)) {
                _.keys(node).forEach(key => {
                    const newKey = !YML_IGNORE_KEYS.includes(key) ? replace(key) : key;
                    const value = node[key];

                    delete node[key];
                    node[newKey] = value;

                    if (!YML_IGNORE_KEYS.includes(key)) {
                        traverse(node[newKey]);
                    }
                });
            }
        };

        YML_SPECS_TYPE_KEYS.forEach(key => traverse(data[key]));

        return data;
    },
    hermione: ast => {
        ast
            .find(j.CallExpression)
            .filter(path =>
                CALLEE_NAMES.includes(_.get(path, 'node.callee.name')) ||
                // поддержка h.describe и h.it
                CALLEE_NAMES.includes(_.get(path, 'node.callee.property.name'))
            )
            .forEach(path => {
                astReplace(_.get(path, 'node.arguments.0'), path, null, replace, true);
            });

        return ast;
    }
};
