"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.parseArgv = void 0;

var _yargs = _interopRequireDefault(require("yargs"));

var _ramda = require("ramda");

var _cosmiconfig = _interopRequireDefault(require("cosmiconfig"));

var _package = require("../package.json");

var _args = require("./args");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

const explorer = (0, _cosmiconfig.default)(_package.name
/*, cosmiconfigOptions */
);

const loadConfig = async filepath => explorer.load(filepath).then(r => r && r.config || {}).catch(() => ({}));

const buildArgv = ({
  usage,
  options = [],
  check
}) => {
  const rawArgv = process.argv.slice(2);
  const argv = (0, _yargs.default)(rawArgv).usage(usage).version(_package.version).options((0, _args.getOptions)(options)).alias('h', 'help').argv; // Проверяем отдельно, т.к. типизация иначе страдает

  let checkFn = argv => (0, _args.checkOptions)(argv);

  if (typeof check === 'function') {
    checkFn = argv => (0, _args.checkOptions)(argv) && check(argv);
  }

  try {
    checkFn(argv);
  } catch (error) {
    console.log(error.message || error);
    process.exit(1);
  }

  return argv;
};

const parseArgv = async argvOptions => {
  let _buildArgv = buildArgv(argvOptions),
      {
    config: pathToConfig = _args.defaults.config
  } = _buildArgv,
      argv = _objectWithoutProperties(_buildArgv, ["config"]);

  if (pathToConfig) {
    const config = await loadConfig(pathToConfig);
    argv = (0, _ramda.mergeAll)([_args.defaults, config, argv]);
  }

  return argv;
};

exports.parseArgv = parseArgv;