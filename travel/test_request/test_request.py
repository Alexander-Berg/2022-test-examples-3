import argparse
import logging
import os

from lxml import etree
import requests

from travel.library.python.tvm_ticket_provider import TvmTicketProvider, TvmClient

logger = logging.getLogger(__name__)


def get_user_uid_and_ticket(sessionid, userip, host, secret):
    passport_tvm_provider = TvmTicketProvider(
        client=TvmClient(
            source_id=int(os.getenv('AVIA_FRONTEND_TVM_SERVICE_ID')),
            secret=secret,
            logger=logger,
            destinations=[224],
        ),
        destinations=[224],
        logger=logger,
    )

    response = requests.get(
        'http://pass-test.yandex.ru/blackbox',
        headers={
            'X-Ya-Service-Ticket': passport_tvm_provider.get_ticket(224),
        },
        params={
            'method': 'sessionid',
            'sessionid': sessionid,
            'userip': userip,
            'host': host,
            'get_user_ticket': 'yes',
        },
    )

    response.raise_for_status()
    xml = etree.fromstring(response.content)
    error = xml.find('error')

    if error.text != 'OK':
        print(response.content)
        raise RuntimeError(error.text)

    return xml.find('uid').text, xml.find('user_ticket').text


def get_service_ticket(secret):
    tvm_provider = TvmTicketProvider(
        client=TvmClient(
            source_id=int(os.getenv('AVIA_FRONTEND_TVM_SERVICE_ID')),
            secret=secret,
            logger=logger,
            destinations=[int(os.getenv('AVIA_TRAVELERS_TVM_SERVICE_ID'))],
        ),
        destinations=[int(os.getenv('AVIA_TRAVELERS_TVM_SERVICE_ID'))],
        logger=logger,
    )

    return tvm_provider.get_ticket(int(os.getenv('AVIA_TRAVELERS_TVM_SERVICE_ID')))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--debug', action='store_const', const=True)

    parser.add_argument('--sessionid', type=str)
    parser.add_argument('--userip', type=str)
    parser.add_argument('--userhost', type=str, default='travel-test.yandex.ru')
    parser.add_argument('--avia_frontend_tvm_secret', type=str, required=True)

    parser.add_argument('--api_uri', type=str, default='http://travelers.testing.avia.yandex.net/v1')
    parser.add_argument('--api_path', type=str, default='/travelers/{uid}')
    parser.add_argument('--method', type=str, default='get')
    parser.add_argument('--data', type=str, default='{}')

    args = parser.parse_args()

    uid, user_ticket = get_user_uid_and_ticket(
        args.sessionid,
        args.userip,
        args.userhost,
        args.avia_frontend_tvm_secret,
    )

    if args.debug:
        print('user uid: {}'.format(uid))
        print('user ticket: {}'.format(user_ticket))

    response = requests.request(
        args.method,
        args.api_uri + args.api_path.format(uid=uid),
        headers={
            'X-Ya-User-Ticket': user_ticket,
            'X-Ya-Service-Ticket': get_service_ticket(args.avia_frontend_tvm_secret),
        },
        data=args.data,
    )

    try:
        response.raise_for_status()
    except Exception as e:
        print(e)

    print(response.content.decode('utf-8'))


if __name__ == '__main__':
    main()
