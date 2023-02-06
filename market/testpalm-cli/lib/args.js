"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.checkOptions = exports.getOptions = exports.options = exports.defaults = void 0;

var _ramda = require("ramda");

var _package = require("../package.json");

const defaults = {
  config: `${_package.name}.config.js` // ...
  // Не подствлявляем default тут, т.к. иначе они всегда буду перезатирать значения из конфига

};
exports.defaults = defaults;
const options = {
  config: {
    alias: 'c',
    description: `The path to a ${_package.name} config file.`,
    type: 'string'
  },
  statface: {
    description: 'Путь к отчету. Должен включать проект и путь внутри проекта.',
    alias: 's',
    type: 'string'
  },
  verbose: {
    alias: 'v',
    type: 'boolean'
  },
  command: {
    type: 'string'
  },
  debug: {
    type: 'boolean'
  }
};
exports.options = options;

const getOptions = optionKeys => (0, _ramda.pick)(['config', ...optionKeys], options);

exports.getOptions = getOptions;

const checkOptions = argv => {
  if (argv.config && !argv.config.match(/\.js(on)?$/)) {
    throw new Error('The --config option requires a file path with a .js or .json extension.\n' + `Example usage: ${_package.name} --config ./${_package.name}.config.js`);
  }

  return true;
};

exports.checkOptions = checkOptions;