import datetime
import os
import time
from pprint import pprint

from pyyamail.user import User


def load(user: User, platform: str):
    sent_count = 100
    subject2time = {}
    subject2mid = {}
    for i in range(0, sent_count):
        current_time = time.time()
        t = datetime.datetime.fromtimestamp(current_time)
        subject = t.strftime('%Y-%m-%d %H:%M:%S.%f')
        subject2time[subject] = current_time
        print(f'Sending mail #{i} with subject {subject}')
        print(user.send_message(subject=subject, to='pastapiz@yandex.ru'))
        find_last_mids(user, subject2mid)
        time.sleep(0.5)
    wait_time = 120
    print(f'Waiting last push receive for {wait_time}s')
    time.sleep(wait_time)

    find_last_mids(user, subject2mid)

    mid2time = {}
    for subject, mid in subject2mid.items():
        if subject in subject2time:
            mid2time[mid] = subject2time[subject]

    if platform == 'android':
        set_android_home = 'export ANDROID_HOME=/Users/$USER/Library/Android/sdk && export PATH=${PATH}:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools'
        os.system(f'{set_android_home} && adb pull /storage/emulated/0/Android/data/ru.yandex.mail.beta/cache/log.txt .')
        os.system(f'{set_android_home} && adb shell rm -f /storage/emulated/0/Android/data/ru.yandex.mail.beta/cache/log.txt')
        log_path = 'log.txt'
    else:
        ios_deploy_home = '/Users/amosov-f/Documents/yandex/ios-deploy/node_modules/ios-deploy/build/Release/ios-deploy'
        os.system(f'{ios_deploy_home} -1 "ru.yandex.mail.debug" --download=/tmp/log.txt --to .')
        log_path = 'tmp/log.txt'

    mid2count = {}
    with open(log_path, 'r') as f:
        for line in f.readlines():
            mid, timestamp = line.split('\t')
            if mid in mid2time:
                receive_time = int(timestamp) / 1000 - mid2time[mid]
                print(f'Push {mid} received in {int(receive_time)} s')
                if mid not in mid2count:
                    mid2count[mid] = 0
                mid2count[mid] += 1

    duplicates = []
    mid2count.values()
    for mid, count in mid2count.items():
        if count > 1:
            duplicates.append(mid)

    print(f'Not received mids {set(mid2time.keys()) - set(mid2count.keys())}')
    print(f'Delivery rate {100 * len(mid2count) / sent_count}%')
    if mid2count:
        print(f'Duplicates {100 * len(duplicates) / len(mid2count)}% mids {duplicates}')


def find_last_mids(user: User, subject_to_mid: dict):
    for message in user.messages_in_folders(fids=["1"], limit=100):
        subj = message['subjText']
        mid = message['mid']
        if not subj in subject_to_mid:
            subject_to_mid[subj] = mid
            print(f'Subject {subj} has mid {mid}')


def test(user: User):
    t = datetime.datetime.fromtimestamp(time.time())
    subject = t.strftime('%Y-%m-%d %H:%M:%S')
    print(f'Subject: {subject}')

    pprint(user.send_message(subject=subject, to='pastapiz@yandex.ru'))


def main():
    user = User(token='***')
    load(user, platform='ios')


if __name__ == '__main__':
    main()