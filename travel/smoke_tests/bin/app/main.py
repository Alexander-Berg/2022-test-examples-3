import logging

from travel.rasp.smoke_tests.smoke_tests.run_smoke_tests import run


log = logging.getLogger(__name__)


def main():
    log.setLevel(logging.INFO)

    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('-c', '--config', dest='config_module')
    parser.add_argument('-e', '--env', dest='env_name')
    parser.add_argument('-p', '--envparams', dest='envparams', default=None)
    parser.add_argument('-s', '--stableness', dest='stableness', default=None)
    args = parser.parse_args()

    run(args.config_module, args.env_name, args.stableness, envparams=args.envparams)


if __name__ == '__main__':
    main()
