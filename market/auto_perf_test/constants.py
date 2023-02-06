# -*- coding: utf-8 -*-

CLUSTER_SIZE = 8
SNIPPET_HOST_INDEX = 8
DEFAULT_TICKETS = ('MARKETOUT-11539', 'MARKETOUT-14633', 'MARKETOUT-14633')
CUSTOM_PORT = 17056
MODEL_META_SEARCH_PORT_OFFSET = 10
MODEL_BASE_SEARCH_PORT_OFFSET = 11
LOG_DIR = '/var/log/search'
DEFAULT_PERF_EXECUTABLE_PATH = '/usr/lib/yandex/perf-test-tools/perf'
DEFAULT_PERF_HOST_INDEX = 0
DEFAULT_FLAME_GRAPH_PATH = '/usr/lib/yandex/perf-test-tools'
DEFAULT_PERF_RECORD_TIME = 60
DEFAULT_PERF_RECORD_DELAY = 0
AMMO_DATE_RE = r'^\d{8}$'
REPORT_ERROR_COUNT_THRESHOLD = 3
DEFAULT_ARTIFACTS_DIR = '~/auto_perf_test'
PHOUT_LOG = 'phout.log'
PERCENTILES = (50, 75, 80, 85, 90, 95, 98, 99, 99.5, 99.8, 99.9)
CONFIG_FILE_NAME = 'config'
ERROR_FILE = 'error'
WEB_LINK_FILE = 'web_link'
CTRL_CHARS = ' \r\n'
PERF_FOLDED_STACKS = 'perf-folded-stacks'
GET_REPORT_PID_COMMAND = r'pgrep -x report || pgrep -f "/usr/bin/market-(snippet-)?report\.httpsearch"'
RED = '\033[1;31m'
RESET = '\033[0;0m'
ESC_CODES = (RED, RESET)
REPORT_MAIN = 0
REPORT_PARALLEL = 1
REPORT_API = 2
REPORT_INT = 3
REPORT_BLUE_MAIN = 4
REPORT_TYPES = (REPORT_MAIN, REPORT_PARALLEL, REPORT_API, REPORT_INT, REPORT_BLUE_MAIN)
REPORT_NAMES = ('main', 'parallel', 'api', 'int', 'blue_main')
DEFAULT_TEST_COUNT = (0, 0, 0, 0, 0)
DEFAULT_WARMUP_RPS_SCHED = (
    'const(20,150s)',
    'const(100,90s)',
    'const(20,150s)',
    'const(100,90s)',
    'const(20,150s)',
)
DEFAULT_RPS_SCHED = (
    'line(1,10,10s) const(10,5m)',
    'line(1,50,10s) const(50,3m)',
    'line(1,10,10s) const(10,5m)',
    'line(1,50,10s) const(50,3m)',
    'line(1,10,10s) const(10,5m)',
)
MAIN_LOGS = (
    'market-access-tskv.log',
    'market-error.log',
    'market-exec-stats.log'
)
PARALLEL_LOGS = MAIN_LOGS
REPORT_LOG_FILE_NAMES = (MAIN_LOGS, PARALLEL_LOGS, MAIN_LOGS, MAIN_LOGS, MAIN_LOGS)
DEFAULT_AMMO_DIR = (
    '/home/lunapark/mainreport/ammo',
    '/home/lunapark/parallel/ammo',
    '/home/lunapark/api/ammo',
    '/home/lunapark/int/ammo',
    '/home/lunapark/blue-main/ammo',
)
AMMO_PREFIXES = ('main', 'parallel', 'api', 'int')
DIFF_ROUNDING_PRECISION = 2
DEFAULT_SUPPRESS_DIFF_THRESHOLD = 0.0001
DEFAULT_WARNING_THRESHOLD = 0.02
ALLOWED_HP_CLUSTERS = (2,)
REPORT_SUBROLES = ('market', 'parallel', 'api', 'int', 'blue-market')
