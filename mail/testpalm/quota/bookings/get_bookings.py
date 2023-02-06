import argparse
import json
import logging
import os
from datetime import datetime
from typing import List, Dict
import requests
from constants import BookingItem, Quota, BookingStatus

logger = logging.getLogger('booking_quota')
logging.basicConfig(format=u'%(asctime)s [%(levelname)s] %(module)s: %(message)s', level=logging.INFO)


def handle_options():
    parser = argparse.ArgumentParser()
    parser.add_argument("-t", "--token", dest="token", default=os.environ['OAUTH_TOKEN_BOOKING'])
    parser.add_argument("-q", "--quota", dest="quota", default=Quota.mobilemail_android)
    parser.add_argument("-s", "--status", dest="status", default=BookingStatus.closed)
    parser.add_argument("-sd", "--start_date", dest="start_date", default='2022-04-01')
    parser.add_argument("-ed", "--end_date", dest="end_date", default='2022-07-01')
    return parser


def str_to_ts(date_str: str) -> int:
    return int(datetime.strptime(date_str, '%Y-%m-%d').timestamp() * 1000)


def get_bookings(args) -> json:
    url = "https://booking.yandex-team.ru/api/bookings/assessor-testing/default/filter"
    data = {
        "status": args.status,
        "quotaSource": args.quota,
        "startFromTs": str_to_ts(args.start_date),
        "startToTs": str_to_ts(args.end_date),
    }
    response = requests.post(
        url,
        data=json.dumps(data),
        headers={
            "Authorization": "OAuth " + args.token,
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
        verify=os.path.join(os.path.dirname(__file__), 'YandexInternalRootCA.crt'),
        timeout=40,
    )
    if response.status_code != 200:
        logging.info(response.status_code)
        logging.info(response.json())
        return None
    return response.json()


def paint(booking_list: List[BookingItem], quota: str) -> str:
    html = '<html><head><meta charset="utf-8"><title>Выполненные брони</title></head><body>'
    html += '<table border="1" align="center"><caption>'
    html += f'Квота {quota}<br>'
    html += '</caption>'
    html += '<tr><th>date</th><th>id</th><th>title</th><th>status</th><th>booked_quota</th><th>spent_quota</th><th>diff</th></tr>\n'
    for booking in booking_list:
        html += f'<tr align="center"> ' \
                f'<td >{booking.date}</td> ' \
                f'<td >{booking.id}</td> ' \
                f'<td align="right">{booking.title}</td> ' \
                f'<td>{booking.status}</td> ' \
                f'<td>{booking.booked_quota}</td> ' \
                f'<td>{booking.spent_quota}</td>' \
                f'<td>{round((booking.spent_quota - booking.booked_quota) / booking.booked_quota * 100, 2)} %</td></tr>\n'
    html += '</table></body></html>'
    return html


def prepare_data(data: List[Dict]) -> List[BookingItem]:
    return list(map(lambda x: BookingItem(
        datetime.fromtimestamp(int(x['estimate']['startTs'] / 1000)),
        x['bookingId'],
        x['title'],
        x['status'],
        x['estimate']['resourceVolume'],
        round(x['cost']['finalQuota'] / (x['estimate']['maxQuota'] / x['estimate']['resourceVolume']), 2)
    ), data))


if __name__ == '__main__':
    args = handle_options().parse_args()
    with open(f'bookings_{args.quota}.html', 'w') as f:
        f.write(paint(prepare_data(get_bookings(args)), args.quota))
