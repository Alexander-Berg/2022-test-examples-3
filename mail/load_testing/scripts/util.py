import argparse

from mail.template_master.load_testing.scripts.constants import RequestTypes


class ToRequestType(argparse.Action):
    def _find_enumerator_with_name(sel, name: str):
        for req_type in RequestTypes:
            if name == req_type.name:
                return req_type
        assert False

    def __call__(self, parser, namespace, values, option_string=None):
        setattr(namespace, self.dest, self._find_enumerator_with_name(values))


def get_cmd_args():
    parser = argparse.ArgumentParser(
        description="Generates requests for template-master's handlers")
    parser.add_argument('--ammo-count',
                        dest='ammo_count',
                        help='number of requests you want to generate',
                        required=True,
                        type=int)
    parser.add_argument('--case-tag',
                        dest='case_tag',
                        help='marker for the test using this dataset',
                        required=False,
                        type=str)
    parser.add_argument('--out-path',
                        dest='out_path',
                        help='output path for the resulting ammo file',
                        required=True,
                        type=str)
    parser.add_argument('--req-type',
                        action=ToRequestType,
                        dest='req_type',
                        help='target handler',
                        required=True,
                        choices=[item.name for item in RequestTypes])
    parser.add_argument('--url', help='target url', required=True, type=str)
    return parser.parse_args()
