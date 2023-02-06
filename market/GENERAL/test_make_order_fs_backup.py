#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty
import os
import json


class T(env.TestSuite):
    """ Check that if YT is unavailable, logs will be written into the file system """
    event_log_path = '//home/event_log'

    @classmethod
    def prepare(cls):
        cls.click_n_collect.with_yt = True
        cls.click_n_collect.with_goods_server = True
        cls.click_n_collect.goods_server.config.Prices['item1'] = 10
        cls.click_n_collect.goods_server.config.Remainings['item1'] = 1
        cls.click_n_collect.goods_server.config.Locations['loc1'].Address = 'Washington beach 13, Vice City'

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
        # nobody expects trainings in sasovo!
        cls.click_n_collect.yt.stop()

    def test_simple(self):
        request = {
            'locationId': 'loc1',
            'merchantId': '12',
            'clickNCollectId': 'item1',
            'quantity': 5,
            'customer': {
                'comment': 'Комментарий',
                'firstName': 'Имя',
                'lastName': 'Фамилия',
                'thirdName': 'Отчество',
                'email': 'ivanov@gyandex.ru',
                'phone': '79261830411'
            },
            'marketOfferInfo': {
                'offerId': '1',
                'feedId': '10',
                'price': 100,
                'showUid': '10001110101100101',
            },
            'user': {
                'yandexuid': '1'
            }
        }
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request))
        self.assertFragmentIn(
            response,
            {
                'status': 'ok',
                'data': {
                    'orderId': NotEmpty()
                }
            },
        )

    @classmethod
    def after_server_stop(cls):
        backups_path = T.click_n_collect.config.YtEventLogWriterTask.FsBackupLogPath
        backups = os.listdir(backups_path)
        assert len(backups) == 1
        expected = {
            'Events': [
                {
                    'Timestamp': NotEmpty(),
                    'UserInfo': {'YandexUid': '1'},
                    'OrderInfo': {
                        'Success': True,
                        'Request': {
                            'LocationId': 'loc1',
                            'MerchantId': '12',
                            'ClickNCollectId': 'item1',
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
        T.check_fs_file_contains(os.path.join(backups_path, backups[0]), expected)


if __name__ == '__main__':
    env.main()
