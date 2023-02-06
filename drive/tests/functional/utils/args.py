import argparse


def get_args():
    parser = argparse.ArgumentParser(description="Drive functional tests")
    parser.add_argument('-c', "--client-public-token", dest="client_public_token", metavar="OAUTH", type=str,
                        default=None,
                        help="Yandex.Passport OAuth token for client")
    parser.add_argument('-ch', "--client-helper-public-token", dest="client_helper_public_token", metavar="OAUTH",
                        type=str, default=None,
                        help="Yandex.Passport OAuth token for client-helper")
    parser.add_argument('-s', "--private-token", dest="private_token", metavar="OAUTH", type=str, default=None,
                        help="Internal Passport OAuth token")
    parser.add_argument('-v', "--verbose", dest="verbose", action="store_true", help="enable debug level logging")
    parser.add_argument("--stop-on-failure", dest="stop_on_failure", action="store_true",
                        help="stop further tests executing if an error is encountered")
    parser.add_argument("--endpoint", dest="endpoint", metavar="server_name", type=str,
                        default="prestable", help="Drive.Frontend host name")
    parser.add_argument("--list-tests", dest="list_tests", action="store_true", help="list existing tests and exit")
    parser.add_argument("-t", "--tests", nargs='+', dest="tests", metavar='tests', type=str,
                        help="run tests(ex. suite1::test1 suite1::test2)")
    parser.add_argument("--tags", nargs='+', dest="tags", metavar='tags', type=str,
                        help="run tests included tags")
    parser.add_argument("--suites", dest="suites", nargs='+', metavar="suites", type=str,
                        help="run suites(ex.suite1 suite2)")
    parser.add_argument("-ds", "--disable_suites", dest="disable_suites", nargs='+', metavar="disable_suites", type=str,
                        help="disable suites(ex.suite1 suite2)")
    parser.add_argument("-to", "--timeout-per-test", dest="timeout_per_test", nargs='?', type=int, default=600,
                        help="time out per test in seconds")

    return parser.parse_args()
