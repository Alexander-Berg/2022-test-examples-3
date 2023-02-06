'use strict';

// В files должны быть только пути файлов тестов или TODO

const _ = require('lodash');
const path = require('path');

const { YML_EXPECTED_FILE_EXTNAMES: EXTS } = require('./../constants');

// TODO: Исключать несуществующие файлы
module.exports = {
    testpalm: data => {
        const comments = [];
        const filesWithoutComments = [];
        const replacedFiles = [].concat(data.files)
            .filter((value, i) => {
                switch(typeof value) {
                    case 'string':
                        if (EXTS.includes(path.extname(value))) {
                            filesWithoutComments.push(value);
                            return true;
                        }
                    case 'object':
                        if (_.keys(value)[0].includes('%%COMMENT')) {
                            _.values(value).forEach((keyValue, j) => {
                                comments.push({ [`%%COMMENT_FILES${i}${j}%%`]: keyValue });
                            });
                        } else {
                            filesWithoutComments.push(value);
                        }

                        return true;
                }
            });

        if (filesWithoutComments.length) {
            data.files = replacedFiles;

            return data;
        } else {
            return _.keys(data).reduce((newData, key) => {
                if (key === 'files') {
                    comments.forEach(value => newData[_.keys(value)[0]] = _.values(value)[0]);
                    newData.files = [];
                } else {
                    newData[key] = data[key];
                }

                return newData;
            }, {});
        }
    },
    hermione: ast => ast
};
