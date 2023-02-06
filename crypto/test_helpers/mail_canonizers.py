import email.parser

import yatest.common


def canonize_mails(output_files, key):
    result = {}
    parser = email.parser.Parser()

    for output_file in output_files:
        with open(output_file) as f:
            mail = parser.parse(f)

        result[key(mail)] = yatest.common.canonical_file(output_file, local=True)

    return result


def canonize_mails_by_subject(output_files):
    return canonize_mails(output_files, key=lambda mail: mail["Subject"])


def canonize_mails_by_addressee(output_files):
    return canonize_mails(output_files, key=lambda mail: mail["To"])
