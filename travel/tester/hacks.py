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
        lines = [l if isinstance(l, unicode) else l.decode('utf-8', 'ignore') for l in lines]
        result = util._format_lines(lines)
        return util.u('\n').join(result)

    util.format_explanation = format_explanation
