import datetime
import time

from pyyamail.user import User


def send_mail(user: User):
    t = datetime.datetime.fromtimestamp(time.time())
    subject = t.strftime('%Y-%m-%d %H:%M:%S')
    to = 'pastapiz@yandex.ru'
    print(f'Sending email with subject "{subject}" to {to}')
    response = user.send_message(subject=subject, to=to)
    print(f'Response from backend: {response}')


def main():
    user = User(token='***')
    for i in range(5):
        send_mail(user)
        time.sleep(1)


if __name__ == '__main__':
    main()