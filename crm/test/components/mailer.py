import smtplib

HOST = "outbound-relay.yandex.net"
PORT = 25
SMTP_TO = "robot-tcrm-test-sp@yandex-team.ru"
DEFAULT_TEXT = "Test test test"
MANDATORY_HEADERS = {'From': 'default-test-address@yandex-team.ru', 'To': 'robot-tcrm-test-sp@yandex-team.ru', 'Subject': 'Default subject by autotests'}


def generate_letter(body, **kwargs):
    letter_headers = kwargs
    for header in MANDATORY_HEADERS:
        if header not in letter_headers:
            letter_headers[header] = MANDATORY_HEADERS[header]
    body = body if body is not None else DEFAULT_TEXT
    letter = '\r\n'.join(list(map(': '.join, letter_headers.items())) + ["", body])
    return letter


def send_mail(body=None, **kwargs):
    server = smtplib.SMTP(HOST, PORT)
    letter = generate_letter(body, **kwargs)
    server.sendmail('this_param_doesnt_matter@yandex-team.ru', SMTP_TO, letter.encode('utf-8'))
    server.quit()
