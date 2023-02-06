# coding: utf-8
import logging
import sys
import os
import json
import textwrap


def main():
    file_name = sys.argv[1]
    if not os.path.exists(file_name):
        raise Exception('Template `{}` does not exist'.format(file_name))
    _set_logger(logging_level=logging.WARNING)

    template = json.loads(_read_file(file_name))
    collector = CheckersCollector(file_name)
    collector.process_node(template)
    collector.verify()


_CHECKER_TEMPLATE = textwrap.dedent(
    """
    var leftAlert = 1;
    var rightAlert = 2;
    var left = 0.001;
    var right = 0.111;
    var filterExternalId = 'qqq';
    var signification = 'RED';
    var percent = 0.5;
    var diff = 0.1;
    var componentFilter = 'skipRightAlign';
    var queryAmount = 6000;
    var alert = true;
    var self = {{
        'judged5': {{
            'left': 10
        }},
        'judged-video-mobile-5': {{
            'left': 10
        }},

    }};
    // checked expressions below
    {body}
    """
)

_EXPRESSION_TEMPLATE = 'var expr_{name}_{index} = {expression};'
_JS_CANDIDATES = ["js", "node", "nodejs"]


class CheckersCollector(object):
    CRIT_EXPR = frozenset(["criticalCheckerJs", "fatalCheckerJs"])
    CRIT_METRICAS = frozenset(["proxima2020"])

    def __init__(self, file_name):
        self.file_name = file_name
        self.expressions = []
        self._detect_js()

    def process_node(self, node):
        if isinstance(node, dict):
            expressions = []
            name = "UNKNOWN"
            for k, v in node.items():
                if 'CheckerJs' in k:
                    if v is not None:
                        expressions.append((k, v.replace('this', 'self')))
                else:
                    self.process_node(v)
                if k == 'name':
                    name = v
            if expressions:
                self.expressions.append((name, expressions))

        elif isinstance(node, list):
            for list_item in node:
                self.process_node(list_item)

    def verify(self):
        logging.info("Start verification %s", self.file_name)
        self._validate_expressions()
        self._check_non_empty_crits()
        logging.info(('Verified {:3} expressions in `{}`'.format(len(self.expressions), self.file_name)))

    def _check_non_empty_crits(self):
        for name, expr in self.expressions:
            if self._metrica_is_important(name):
                if not any(expr_name in self.CRIT_EXPR for expr_name, _ in expr):
                    logging.warning(
                        "[%s] Metrica '%s' does not have crit or fatal checker. Checkers: %s",
                        self.file_name, name, expr
                    )
                else:
                    logging.debug("[%s] Metrica '%s' OK", self.file_name, name)

    def _detect_js(self):
        for name in _JS_CANDIDATES:
            if os.system("which {} >/dev/null".format(name)) == 0:
                self.js = name
                return self.js
        raise Exception(
            "Cannot locate JS binary. Candidates were: {}. Please install nodejs.".format(
                ", ".join(_JS_CANDIDATES)
            )
        )

    def _validate_expressions(self):
        body = '\n'.join([
            _EXPRESSION_TEMPLATE.format(name=name.replace("-", "_"), index=index, expression=expression)
            for name, expr in self.expressions for index, expression in expr
        ])
        # logging.debug("Checking...\n'%s'", body)

        contents = _CHECKER_TEMPLATE.format(body=body)

        js_name = '_tmp.js'
        log_name = 'js_output.err.log'

        _write_file(js_name, contents)
        if os.system('{} {} >{} 2>&1'.format(self.js, js_name, log_name)) != 0:
            logging.error((_read_file(log_name)))
            os.unlink(log_name)
            raise Exception('Verify expression `{}` failed. '.format(contents))

        os.unlink(log_name)
        os.unlink(js_name)

    def _metrica_is_important(self, metrica_name):
        # return True
        return metrica_name in self.CRIT_METRICAS


def _read_file(file_name):
    with open(file_name) as f:
        return f.read()


def _write_file(file_name, contents):
    with open(file_name, 'w') as f:
        f.write(contents)


def _set_logger(logging_level=None):
    logger = logging.getLogger()
    if logging_level:
        logger.setLevel(logging_level)
    handler = logging.StreamHandler(sys.stderr)
    handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s - %(message)s'))
    logger.handlers = [handler]


if __name__ == '__main__':
    main()
