from .parsexml import ParsexmlTestData

import subprocess

SANDBOX_API_URL = "https://sandbox.yandex-team.ru:443/api/v1.0"
_generators = {
    'parsexml': ParsexmlTestData,
}


def parse_args():
    from argparse import ArgumentParser
    parser = ArgumentParser()
    subs = parser.add_subparsers(dest='for_')

    for gen_name, gen_cls in _generators.items():
        subparser = subs.add_parser(name=gen_name)
        gen_cls.fill_args(subparser)

    return parser.parse_args()


def main():
    args = parse_args()
    generator = _generators[args.for_](args)
    generator.generate_test_data()


if __name__ == '__main__':
    main()
