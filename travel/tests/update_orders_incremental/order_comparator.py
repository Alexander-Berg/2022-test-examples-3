from typing import NamedTuple, Any


class DiffItem(NamedTuple):
    field: str
    expected: Any
    actual: Any


class OrderComparator:

    __order_fields_to_ignore__ = [
        'first_seen_at',
        'is_suspicious',
        'status_explanation',
        'suspicious_explanation',
        'has_label',
    ]

    @staticmethod
    def compare_pair(table_name, index, expected, actual, fields_to_ignore):

        diff = list()

        for field in set(expected.keys()) | set(actual.keys()):
            if field in fields_to_ignore:
                continue
            expected_value = expected.get(field)
            actual_value = actual.get(field)
            if expected_value != actual_value:
                diff.append(DiffItem(field, expected_value, actual_value))

        if diff:
            diff_text = '\n'.join(str(d) for d in diff)
            raise Exception(f'Table content differs from expected {table_name}[{index}]\n{diff_text}')
