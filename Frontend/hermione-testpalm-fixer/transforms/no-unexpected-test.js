'use strict';

// spec_2: Название кейса должно быть лаконичным. Оно не должно содержать слов, подчеркивающих назначение самих тест-кейсов, например: проверка, корректность и так далее

const _ = require('lodash');
const j = require('jscodeshift');

const { invertDict } = require('../utils');
const { TEST_CALLEE_NAMES, YML_SPECS_TITLE_KEYS, YML_SPECS_TYPE_KEYS, YML_IGNORE_KEYS } = require('../constants');
const wordDict = require('./../dictionaries/word-dictionary');
const { astReplace } = require('./../utils');

const CALLEE_NAMES = TEST_CALLEE_NAMES.slice(1);
const unexpectedWordDict = invertDict(wordDict);
const replace = value => _.keys(unexpectedWordDict).reduce((acc, word) => {
    const invertFirst = word[0] === _.toLower(word[0]) ? _.upperFirst : _.lowerFirst;

    if (acc.includes(word) || acc.includes(invertFirst(word))) {
        return acc
            .replace(new RegExp(word, 'g'), unexpectedWordDict[word])
            .replace(new RegExp(invertFirst(word), 'g'), invertFirst(unexpectedWordDict[word]));
    }

    return acc;
}, String(value));

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
                astReplace(_.get(path, 'node.arguments.0'), path, null, replace);
            });

        return ast;
    }
};
