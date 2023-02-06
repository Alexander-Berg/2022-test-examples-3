# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import six


def apply_format_explanation():
    from _pytest.assertion import util

    def format_explanation(explanation):
        """This formats an explanation

        Normally all embedded newlines are escaped, however there are
        three exceptions: \n{, \n} and \n~.  The first two are intended
        cover nested explanations, see function and attribute explanations
        for examples (.visit_Call(), visit_Attribute()).  The last one is
        for when one explanation needs to span multiple lines, e.g. when
        displaying diffs.
        """
        explanation = util._collapse_false(explanation)
        lines = util._split_explanation(explanation)
        lines = [l if isinstance(l, six.text_type) else l.decode('utf-8', 'ignore') for l in lines]
        result = util._format_lines(lines)
        return util.u('\n').join(result)

    util.format_explanation = format_explanation


def disable_httpretty_for_db_connection():
    from travel.rasp.library.python.common23.db import connect
    from httpretty import httpretty

    original_get_connection = connect.get_connection

    def patched_get_connection(*args, **kwargs):
        httpretty_enabled = httpretty.is_enabled()
        try:
            httpretty.disable()
            return original_get_connection(*args, **kwargs)
        finally:
            if httpretty_enabled:
                httpretty.enable()

    connect.get_connection = patched_get_connection
