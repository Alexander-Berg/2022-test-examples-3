# coding=utf-8
import logging
import os

from lib.adgroups_api import AdGroupsApi
from lib.bid_modifiers_api import BidModifiersApi
from lib.campaigns_api import CampaignsApi
from lib.regions import REGIONS

logging.basicConfig()
logger = logging.getLogger(__name__)
logger.setLevel('DEBUG')


class LoadTest(object):
    def __init__(self, gun):
        self.gun = gun

        self.token = self.gun.get_option('token')
        if not self.token:
            raise RuntimeError('Missing required option `token`')

        self.client_login = self.gun.get_option('client_login')
        if not self.client_login:
            raise RuntimeError('Missing required option `client_login`')

        self.old_api_url = self.gun.get_option('old_api_url')
        if not self.old_api_url:
            raise RuntimeError('Missing required option `old_api_url`')

        self.java_api_url = self.gun.get_option('java_api_url')
        self.use_always_old_api = self.gun.get_option('use_always_old_api', 'False') == 'True'
        if not self.java_api_url and not self.use_always_old_api:
            raise RuntimeError('Missing required option `java_api_url`')

        self.campaigns_api = CampaignsApi(
            self.gun.measure,
            java_api_url=self.java_api_url, old_api_url=self.old_api_url,
            client_login=self.client_login, token=self.token, use_always_old_api=self.use_always_old_api)
        self.adgroups_api = AdGroupsApi(
            self.gun.measure,
            java_api_url=self.java_api_url, old_api_url=self.old_api_url,
            client_login=self.client_login, token=self.token, use_always_old_api=self.use_always_old_api)
        self.bid_modifiers_api = BidModifiersApi(
            self.gun.measure,
            java_api_url=self.java_api_url, old_api_url=self.old_api_url,
            client_login=self.client_login, token=self.token, use_always_old_api=self.use_always_old_api)

        self.campaign_id = None
        self.adgroup_id = None

    def setup(self, param):
        """
        Этот код будет выполнен в каждом воркере перед запуском теста
        Создаём кампанию, а в teardown() удалим её
        """
        self.campaign_id = self.campaigns_api.create_campaign()
        self.adgroup_id = self.adgroups_api.create_adgroup(self.campaign_id)
        logger.info("Setup instance, created campaign %d, created adgroup %d", self.campaign_id, self.adgroup_id)

    def teardown(self):
        """
        This will be executed in each worker after the end of the test
        """
        logger.info("Tearing down.. Trying to delete adgroup %d", self.adgroup_id)
        self.adgroups_api.delete_adgroup(self.adgroup_id)
        logger.info("Tearing down.. Trying to delete campaign %d", self.campaign_id)
        self.campaigns_api.delete_campaign(self.campaign_id)

        os._exit(0)  # It's mandatory to explicitly stop worker process in teardown
        return 0

    def case1(self, missile):
        """
        Добавляем корректировки по одной, потом удаляем все
        """
        mobile_modifier_id = self.bid_modifiers_api.create_bidmodifier_mobile(self.campaign_id)
        demographic_modifier_ids = self.bid_modifiers_api.create_bidmodifiers_demographic(self.campaign_id)
        self.bid_modifiers_api.delete_bidmodifiers(
            [item for sublist in [demographic_modifier_ids, [mobile_modifier_id]]
             for item in sublist])

    def case2(self, missile):
        """
        Добавляем большую гео-корректировку и удаляем
        """
        geo_ids = self.bid_modifiers_api.create_bidmodifiers_geo(self.campaign_id, REGIONS[:100])
        self.bid_modifiers_api.delete_bidmodifiers(geo_ids)

    def case3(self, missile):
        """
        Добавляем большую гео-корректировку, а потом пробуем добавить ещё гео-корректировок
        """
        # Здесь измеряем вручную, чтобы можно было отличить эти два вызова в результатах
        with self.gun.measure("add_many_geo_first_call"):
            geo_ids = self.bid_modifiers_api.create_bidmodifiers_geo(self.campaign_id, REGIONS[:100])
        with self.gun.measure("add_many_geo_second_call"):
            geo_ids = geo_ids + self.bid_modifiers_api.create_bidmodifiers_geo(self.campaign_id, REGIONS[101:200])
        self.bid_modifiers_api.delete_bidmodifiers(geo_ids)

    def case4(self, missile):
        """
        Добавляем корректировки по одной, изменяем, потом удаляем все
        """
        mobile_modifier_id = self.bid_modifiers_api.create_bidmodifier_mobile(self.campaign_id)
        demographic_modifier_ids = self.bid_modifiers_api.create_bidmodifiers_demographic(self.campaign_id)
        self.bid_modifiers_api.set_bidmodifiers(
            [item for sublist in [demographic_modifier_ids, [mobile_modifier_id]]
             for item in sublist])
        self.bid_modifiers_api.delete_bidmodifiers(
            [item for sublist in [demographic_modifier_ids, [mobile_modifier_id]]
             for item in sublist])

        def case5(self, missile):
            """
            Добавляем большую гео-корректировку, изменяем её, потом удаляем все
            """

        geo_ids = self.bid_modifiers_api.create_bidmodifiers_geo(self.campaign_id, REGIONS[:100])
        self.bid_modifiers_api.set_bidmodifiers(geo_ids)
        self.bid_modifiers_api.delete_bidmodifiers(geo_ids)

    def case6(self, missile):
        """
        Добавляем корректировки по одной, включаем/выключаем на группе, потом удаляем все
        """
        demographic_modifier_ids = self.bid_modifiers_api.create_bidmodifiers_demographic(self.campaign_id)
        self.bid_modifiers_api.toggle_bidmodifiers(adgroup_id=self.adgroup_id, enabled=0)
        self.bid_modifiers_api.toggle_bidmodifiers(adgroup_id=self.adgroup_id, enabled=1)
        self.bid_modifiers_api.delete_bidmodifiers(demographic_modifier_ids)

    def case7(self, missile):
        """
        Добавляем большую гео-корректировку, включаем/выключаем, потом удаляем все
        """
        geo_ids = self.bid_modifiers_api.create_bidmodifiers_geo(self.campaign_id, REGIONS[:100])
        bmtype = "REGIONAL_ADJUSTMENT"
        with self.gun.measure("disable_bidmodfiers"):
            self.bid_modifiers_api.toggle_bidmodifiers(campaign_id=self.campaign_id, bmtype=bmtype, enabled=0)
        with self.gun.measure("enabled_bidmodfiers"):
            self.bid_modifiers_api.toggle_bidmodifiers(campaign_id=self.campaign_id, bmtype=bmtype, enabled=1)
        self.bid_modifiers_api.delete_bidmodifiers(geo_ids)

    def case8(self, missile):
        """
        Добавляем корректировки по одной, включаем/выключаем на кампании, потом удаляем все
        """
        demographic_modifier_ids = self.bid_modifiers_api.create_bidmodifiers_demographic(self.campaign_id)
        self.bid_modifiers_api.toggle_bidmodifiers(campaign_id=self.campaign_id, enabled=0)
        self.bid_modifiers_api.toggle_bidmodifiers(campaign_id=self.campaign_id, enabled=1)
        self.bid_modifiers_api.delete_bidmodifiers(demographic_modifier_ids)
