#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty
from google.protobuf.json_format import ParseDict
from market.click_n_collect.proto.event_log_pb2 import TEventLogRows
import os
import datetime


class T(env.TestSuite):
    """ Check files from fs backup uploads into YT"""
    event_log_path = '//home/event_log'

    @classmethod
    def prepare(cls):
        cls.click_n_collect.with_yt = True
        cls.click_n_collect.with_goods_server = True
        cls.click_n_collect.config.MakeOrderHandler = True
        cls.click_n_collect.config.MakeOrderRequester.Host = ''    # it will be filled by beam
        cls.click_n_collect.config.MailSenderRequester.Host = ''    # it will be filled by beam
        cls.click_n_collect.config.MailSender.Authorization = ''    # it will be filled by beam
        cls.click_n_collect.config.MailSender.Url = ''              # it will be filled by beam

        cls.click_n_collect.config.YtEventLogWriterTask.YtLogPath = cls.event_log_path
        cls.click_n_collect.config.YtEventLogWriterTask.FsBackupLogPath = cls.click_n_collect.ctx.work_dir.subdir('event_log_backup').path
        cls.click_n_collect.config.YtEventLogWriterTask.BackupCheckIntervalSec = 1

    @classmethod
    def before_server_start(cls):
        backup = {
            'Events': [
                {
                    'Timestamp': 1234,
                    'UserInfo': {'YandexUid': '1'},
                    'OrderInfo': {
                        'Success': True,
                        'Request': {
                            'LocationId': '125929',
                            'MerchantId': '12',
                            'ClickNCollectId': '100000015724',
                            'Quantity': '5',
                            'CustomerInfo': {
                                'Comment': 'Комментарий',
                                'FirstName': 'Имя',
                                'LastName': 'Фамилия',
                                'ThirdName': 'Отчество',
                                'Email': 'ivanov@gyandex.ru',
                                'Phone': '79261830411'
                            },
                            'MarketOfferInfo': {
                                'OfferId': '1',
                                'FeedId': '10',
                                'Price': 100.0,
                                'ShowUid': '10001110101100101',
                            },
                            'UserInfo': {
                                'YandexUid': '1'
                            }
                        }
                    },
                },
            ]
        }
        msg = TEventLogRows()
        ParseDict(backup, msg)
        backups_path = T.click_n_collect.config.YtEventLogWriterTask.FsBackupLogPath
        with open(os.path.join(backups_path, 'bkp'), 'w') as f:
            f.write(msg.SerializeToString())

    @classmethod
    def after_server_stop(cls):
        backups_path = T.click_n_collect.config.YtEventLogWriterTask.FsBackupLogPath
        backups = os.listdir(backups_path)
        assert len(backups) == 0

        yt = cls.click_n_collect.yt.connect()
        table_path = cls.event_log_path + '/' + datetime.datetime.utcnow().strftime('%Y-%m-%d')
        assert yt.exists(table_path)

        expected = [
            {
                'timestamp': NotEmpty(),
                'user_info': {'YandexUid': '1'},
                'order_info': {
                    'Success': True,
                    'Request': {
                        'LocationId': '125929',
                        'MerchantId': '12',
                        'ClickNCollectId': '100000015724',
                        'Quantity': '5',
                        'CustomerInfo': {
                            'Comment': 'Комментарий',
                            'FirstName': 'Имя',
                            'LastName': 'Фамилия',
                            'ThirdName': 'Отчество',
                            'Email': 'ivanov@gyandex.ru',
                            'Phone': '79261830411'
                        },
                        'MarketOfferInfo': {
                            'OfferId': '1',
                            'FeedId': '10',
                            'Price': 100.0,
                            'ShowUid': '10001110101100101',
                        },
                        'UserInfo': {
                            'YandexUid': '1'
                        }
                    }
                },
            },
        ]

        cls.check_yt_table_contains(yt, table_path, expected)


if __name__ == '__main__':
    env.main()
