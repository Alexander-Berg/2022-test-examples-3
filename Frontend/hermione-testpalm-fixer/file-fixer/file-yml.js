'use strict';

const fs = require('fs');
const path = require('path');
const util = require('util');
const _ = require('lodash');
const parser = require('js-yaml');

const { executeTransforms } = require('./../transforms');
const { YML_SPECS_TITLE_KEYS } = require('./../constants');

const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

const readYmlFile = async filePath => {
    try {
        const specs = await readFile(filePath, 'utf-8');
        const refs = _.uniq(Array.from(specs.matchAll(/[^\S]\*(\S*)/gm)).map(arr => arr[1]));

        let data = {};

        const tryParse = (value, fixIndent, limit = 2) => {
            try {
                data = parser.safeLoad(prenormalizeYml(value, fixIndent));
                data['%%REFS%%'] = refs;
            } catch (e) {
                if (limit - 1) {
                    tryParse(value, true, limit - 1);
                } else {
                    console.log('Parsing error:', filePath);
                    console.error(e);
                }
            }
        }

        tryParse(specs);

        return data;
    } catch (e) {
        console.error(e);
    }
};

const updateYmlFile = async (filePath, data = {}, options = {}) => {
    let replacedData;
    const refs = data['%%REFS%%'];

    delete data['%%REFS%%'];

    try {
        replacedData = executeTransforms('testpalm', _.cloneDeep(data));

        if (!_.isEqual(data, replacedData) && options.rewrite) {
            const replacedSpecs = normalizeYml(
                parser.safeDump(replacedData, { lineWidth: 9999 }),
                { refs }
            );

            await writeFile(filePath, replacedSpecs, 'utf-8');

            if (options.verbose) {
                console.log(`- ${filePath}`);
            }
        }
    } catch (e) {
        console.error(e);
    }

    return replacedData;
};

function prenormalizeYml(value, fixIndent) {
    const arrValue = value.split('\n');

    const getIndent = (line) => line && line.replace(/^(\s*)(.*)$/gm, '$1').length;
    const getLineType = (line) => {
        switch (line && line.trim()[0]) {
            case '#':
                return 'comment';
            case '-':
                return 'array';
            default:
                return 'object';
        }
    };
    const isCommentLine = (line) => getLineType(line) === 'comment';
    const findSiblingLine = (lines, i, step = 1) => {
        const indent = getIndent(lines[i]);
        const tryFind = (toDown = true, withoutIndent = false) => {
            for (let j = i; lines[j] !== undefined; j = toDown ? j + step : j - step) {
                if (!!lines[j] && !isCommentLine(lines[j])) {
                    if (withoutIndent || indent === getIndent(lines[j])) {
                        return lines[j];
                    } else {
                        break;
                    }
                }
            }
        };
        
        return (
            tryFind(true, false) ||
            tryFind(false, false) ||
            tryFind(true, true) ||
            tryFind(false, true)
        );
    };
    const getCommentLineType = (lines, i) => {
        if (i < 0 || i > lines.length - 1) {
            return;
        }

        if (!isCommentLine(lines[i])) {
            return `stringIn${_.upperFirst(getLineType(lines[i]))}`;
        }

        return getLineType(findSiblingLine(lines, i));
    };
    const fixCommentIndent = (lines, i) => {
        const targetIndent = getIndent(findSiblingLine(lines, i));
        const indentValue = targetIndent ? _.times(targetIndent + 1, '').join(' ') : '';
        const newComment = targetIndent ? lines[i].trim().replace('#' + indentValue, '# ') : lines[i];

        return indentValue + newComment;
    }

    return arrValue
        .map((line, i) => {
            let newLine = line;

            if (!newLine.includes('#')) {
                return newLine;
            }

            if (fixIndent && arrValue[i + 1] !== undefined ) {
                newLine = fixCommentIndent(arrValue, i);
            }

            const b = newLine.split('#')[1].includes('"') ? '\'' :  '"';
            const type = getCommentLineType(arrValue, i);
            const prefix = type.includes('array') || type.includes('Array') ? '- ' : '';

            switch (type) {
                case 'stringInArray':
                case 'stringInObject':
                    return newLine
                        .replace(/^(\s*)(.+)\s#\s?(.*)$/, `$1${prefix}'%%COMMENT${i + 1}%%': ${b}$3${b}\n$1$2`);
                case 'array':
                case 'object':
                    return newLine.replace(/^(\s*)#\s?(.*)$/, `$1${prefix}'%%COMMENT${i + 1}%%': ${b}$2${b}`);
                default:
                    return newLine;
            }
        })
        .join('\n');
}

function normalizeYml(value, { refs = [] }) {
    const arrValue = refs
        .reduce((acc, ref, i) => acc.replace(new RegExp(`ref_${i}`, 'gm'), ref), value)
        .split('\n')
        .map(line => {
            return line
                // конвертируем комментарии в массиве или в объекте
                .replace(/^(\s*)(- )?'%%COMMENT.*?%%': ['"]?(.*?)['"]?$/gm, '$1# $3')
                // убираем кавычки вокруг элемента массива или значения свойства объекта
                .replace(/^(.*?[:-]\s)['"]([^\[\{\d\*\.-].*?)['"]$/gm, '$1$2')
                // добавляем кавычки, если в строке есть ': ', чтобы не ломался синтаксис
                .replace(/^([^-:]*?:\s)([^'"].+?:.+?)$/gm, "$1'$2'")
                .replace(/^(.*?\-\s)([^'"].+?:.+?)$/gm, "$1'$2'")
                // убираем кавычки вокруг ключа свойства объекта
                .replace(/^(\s*)'(.*?)'(:)$/gm, '$1$2$3')
                // убираем двойные кавычки-артефакты
                .replace(/\'\'/gm, '\'');
        })
        .filter(Boolean);

    return arrValue
        .map((line, i) => {
            if (
                line.startsWith(' ') ||
                (arrValue[i - 1] || '').startsWith('#') ||
                YML_SPECS_TITLE_KEYS.some(key => line.startsWith(key))
            ) {
                return line;
            }
            return `\n${line}`;
        })
        .join('\n') + '\n';
}

module.exports = {
    readYmlFile,
    updateYmlFile
};
