from datetime import datetime as dt
import texttable


class Method(object):
    def __init__(self, description, action):
        self.description = description
        self.action = action


class Case(object):
    def __init__(self, name, *args, **kwargs):
        self.name = name
        self.args = args
        self.kwargs = kwargs


def run_experiment(clean_up_action, methods, cases):
    timings = {}

    for case in cases:
        for method in methods:
            clean_up_action()
            start_time = dt.now()
            method.action(*case.args, **case.kwargs)
            elapsed_time = dt.now() - start_time

            if method.description not in timings:
                timings[method.description] = {}

            timings[method.description][case.name] = elapsed_time

    table = texttable.Texttable(max_width=0)

    col_dtypes = ["t"]
    header = ["Method"]
    for case in cases:
        col_dtypes.append("a")
        header.append(case.name)

    table.set_cols_dtype(col_dtypes)
    table.add_row(header)

    for method_desc, method_timings in timings.items():
        row = [method_desc]
        for _, case_time in method_timings.items():
            row.append(str(case_time).split('.')[0])

        table.add_row(row)

    print(table.draw())
