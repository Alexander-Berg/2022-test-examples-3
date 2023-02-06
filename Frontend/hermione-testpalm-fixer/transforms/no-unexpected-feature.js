'use strict';

// spec_10: В названии фичи не может содержаться цель тестирования. К примеру, Проверка корректности
// spec_11: В названии фичи не должно встречаться слово функциональность. | Не используем слово Эксперимент в названии эксперимента, и слово Фича в названии фичи. Стараемся давать название, из которого сразу понятен контекст
// Название фичи должно быть включено в разрешенный список

const _ = require('lodash');
const j = require('jscodeshift');

const { invertDict } = require('../utils');
const { TEST_CALLEE_NAMES, YML_SPECS_TITLE_KEYS } = require('../constants');
const featureDict = require('./../dictionaries/feature-dictionary');
const wordDict = require('./../dictionaries/word-dictionary');
const { astReplace, getSpecsTitle, parseSpecsTitle } = require('./../utils');

const KEY = YML_SPECS_TITLE_KEYS[1];
const featureDublicateDict = invertDict(featureDict);
const unexpectedWordDict = invertDict(wordDict);
const replace = value =>
    featureDublicateDict[value] ||
    _.keys(unexpectedWordDict).reduce((acc, word) => {
        const invertFirst = word[0] === _.toLower(word[0]) ? _.upperFirst : _.lowerFirst;
    
        return acc
            .replace(new RegExp(word, 'g'), unexpectedWordDict[word])
            .replace(new RegExp(invertFirst(word), 'g'), invertFirst(unexpectedWordDict[word]));
    }, String(value));

module.exports = {
    testpalm: data => {
        data[KEY] = replace(data[KEY]);

        return data;
    },
    hermione: ast => {
        ast
            .find(j.CallExpression, { callee: { name: TEST_CALLEE_NAMES[0] } })
            .forEach(path => {
                const argNode = _.get(path, 'node.arguments.0');

                if (argNode.type === 'Literal') {
                    const obj = parseSpecsTitle(argNode.value);

                    obj[KEY] = replace(obj[KEY]);

                    argNode.value = getSpecsTitle(obj);
                } else {
                    astReplace(argNode, path, KEY, replace);
                }
            });

        return ast;
    }
};
