"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.run = void 0;

var _axios = _interopRequireDefault(require("axios"));

var _chalk = _interopRequireDefault(require("chalk"));

var _moment = _interopRequireDefault(require("moment"));

var _dedentJs = _interopRequireDefault(require("dedent-js"));

var _invariant = _interopRequireDefault(require("invariant"));

var _ramda = require("ramda");

var _snakeCase = _interopRequireDefault(require("snake-case"));

var _argv = require("./argv");

var _utils = require("./utils");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? Object(arguments[i]) : {}; var ownKeys = Object.keys(source); if (typeof Object.getOwnPropertySymbols === 'function') { ownKeys.push.apply(ownKeys, Object.getOwnPropertySymbols(source).filter(function (sym) { return Object.getOwnPropertyDescriptor(source, sym).enumerable; })); } ownKeys.forEach(function (key) { _defineProperty(target, key, source[key]); }); } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

// @ts-ignore
const TestpalmClient = require('@yandex-int/testpalm-api').default;

const TESTPALM_OAUTH_API_TOKEN = process.env.TESTPALM_OAUTH_API_TOKEN;
const STATFACE_API_TOKEN = process.env.STATFACE_API_TOKEN;
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

const catchError = fn => () => fn().catch(console.error);

const usage = 'Usage: $0 <projectId>';
const byStatus = (0, _ramda.pipe)((0, _ramda.groupBy)(({
  status
}) => status), (0, _ramda.mapObjIndexed)(testcases => testcases.length));

const byAutotest = testCases => ({
  total: testCases.length,
  isAutotest: testCases.filter(({
    isAutotest
  }) => isAutotest).length
});

const normalizeKeys = (0, _utils.mapKeys)(_snakeCase.default);
const formats = {
  text: console.log,
  json: v => console.log(JSON.stringify(v, null, 2))
};

const printResult = (format = 'text') => formats[format];

const testpalmClient = new TestpalmClient(TESTPALM_OAUTH_API_TOKEN);

const getTestPalmStat = projectId => testpalmClient.getTestCases(projectId).then(testCases => normalizeKeys(_objectSpread({}, byAutotest(testCases), byStatus(testCases)))).then(stat => [_objectSpread({
  fielddate: (0, _moment.default)().format('DD.MM.YYYY')
}, stat)]);

const getTestPalmRuns = async projectId => {
  const from = (0, _moment.default)().subtract(1, 'day').startOf('day').valueOf();
  const to = (0, _moment.default)().startOf('day').valueOf();
  return testpalmClient.getTestRuns(projectId, {
    expression: JSON.stringify({
      type: 'AND',
      left: {
        type: 'AND',
        left: {
          type: 'GT',
          key: 'finishedTime',
          value: from
        },
        right: {
          type: 'LT',
          key: 'finishedTime',
          value: to
        }
      },
      right: {
        type: 'AND',
        left: {
          type: 'AND',
          left: {
            type: 'CONTAIN',
            key: 'title',
            value: 'testing-hermione-only'
          },
          right: {
            type: 'AND',
            left: {
              type: 'NOT_CONTAIN',
              key: 'title',
              value: 'broken'
            },
            right: {
              type: 'AND',
              left: {
                type: 'NOT_CONTAIN',
                key: 'title',
                value: 'auto-filter'
              },
              right: {
                type: 'NOT_CONTAIN',
                key: 'title',
                value: 'case-filter'
              }
            }
          }
        },
        right: {
          type: 'EQ',
          key: 'status',
          value: 'FINISHED'
        }
      }
    }),
    limit: 1,
    createdTimeSort: 'desc',
    include: ['executionTime', 'finishedTime', 'startedTime', 'resolution', 'version', 'title']
  }).then(testRuns => testRuns.map(run => {
    const {
      version,
      startedTime,
      finishedTime,
      executionTime,
      resolution
    } = run;
    let start = (0, _moment.default)(startedTime);
    let end = (0, _moment.default)(finishedTime);
    let diff = end.diff(start);

    let formattedExecutionTime = _moment.default.utc(diff).format('HH ч. mm м. ss с.');

    return {
      version,
      autotests_count: resolution.counter.total,
      execution_time: executionTime,
      formatted_execution_time: formattedExecutionTime,
      fielddatetime: (0, _moment.default)(startedTime).format('YYYY-MM-DD HH:MM:00')
    };
  }));
};

const postToStatface = name => values => {
  (0, _invariant.default)(STATFACE_API_TOKEN, 'Env variable "STATFACE_API_TOKEN" is required with --statface option.');
  return _axios.default.post(`https://upload.stat.yandex-team.ru/_api/report/data/${name}`, {
    scale: 'd',
    json_data: JSON.stringify({
      values
    }),
    _append_mode: 1
  }, {
    headers: {
      Authorization: `OAuth ${STATFACE_API_TOKEN}`
    }
  }).then(r => r.data).then(r => r && r.message ? r.message : r);
};

const run = catchError(async () => {
  const argv = await (0, _argv.parseArgv)({
    usage,
    options: ['statface', 'verbose', 'command'],
    check: ({
      _: args,
      $0
    }) => {
      if (!args[0]) {
        throw new Error((0, _dedentJs.default)`
          ${_chalk.default.red('The <projectId> option is required')}
          ${usage.replace('$0', $0).replace('<trackerVersion>', _chalk.default.red('<trackerVersion>'))}
        `);
      }
    }
  });
  const {
    _: args,
    statface: statfaceReportName,
    command,
    verbose
  } = argv;
  const [projectId] = args;
  (0, _invariant.default)(TESTPALM_OAUTH_API_TOKEN, 'Env variable "TESTPALM_OAUTH_API_TOKEN" is required'); // debug && console.log('Options:', argv)
  // verbose && console.log('"--verbose" option passed')

  return (command === 'testruns' ? getTestPalmRuns(projectId) : getTestPalmStat(projectId)).then( // Если нужно вывести и данные и сообщение об отправке данных
  verbose && statfaceReportName ? printResult('json') : v => v).then(statfaceReportName ? postToStatface(statfaceReportName) : projectId => projectId).then(printResult(statfaceReportName ? 'text' : 'json')).catch(console.error);
});
exports.run = run;