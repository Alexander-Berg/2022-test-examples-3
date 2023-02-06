# coding=utf-8
import logging
import random

from lib.base_api import BaseApi, measured

logging.basicConfig()
logger = logging.getLogger(__name__)
logger.setLevel('DEBUG')


class BidModifiersApi(BaseApi):
    def __init__(self, measurer, java_api_url, old_api_url, client_login, token, use_always_old_api=False):
        super(BidModifiersApi, self).__init__(measurer, java_api_url, old_api_url, client_login, token,
                                              use_always_old_api)

    @measured
    def create_bidmodifier_mobile(self, campaign_id):
        """
        Добавляет мобильную корректировку в кампанию campaign_id и возвращает ID созданной корректировки
        """
        result = self.send_request(self.get_java_api_url() + '/json/v5/bidmodifiers/', {
            "method": "add",
            "params": {
                "BidModifiers": [{
                    "CampaignId": campaign_id,
                    "MobileAdjustment": {
                        "BidModifier": 55
                    }
                }]
            }
        })
        return result['result']['AddResults'][0]['Ids'][0]

    @measured
    def create_bidmodifiers_geo(self, campaign_id, regions):
        """
        Добавляет набор геокорректировок в кампанию campaign_id, возвращает список ID созданных корректировок
        :param campaign_id: ID кампании
        :param regions: Какие из регионов добавить, см REGIONS
        """
        result = self.send_request(self.get_java_api_url() + '/json/v5/bidmodifiers/', {
            "method": "add",
            "params": {
                "BidModifiers": [{
                    "CampaignId": campaign_id,
                    "RegionalAdjustments": [{
                        "RegionId": region[0],
                        "BidModifier": 55
                    } for region in regions]
                }]
            }
        })
        return result['result']['AddResults'][0]['Ids']

    @measured
    def create_bidmodifiers_demographic(self, campaign_id, demographics=None):
        """
        Добавляет набор демографических корректировок в кампанию campaign_id, возвращает список ID созданных корректировок
        :param demographics: Настройки демографии, например, [('GENDER_MALE', 'AGE_18_24'), ('GENDER_FEMALE', None)]
        """
        demographics_to_apply = demographics if demographics else [('GENDER_MALE', 'AGE_18_24')]
        result = self.send_request(self.get_java_api_url() + '/json/v5/bidmodifiers/', {
            "method": "add",
            "params": {
                "BidModifiers": [{
                    "CampaignId": campaign_id,
                    "DemographicsAdjustments": [{
                        "Gender": demography[0],
                        "Age": demography[1],
                        "BidModifier": 55
                    } for demography in demographics_to_apply]
                }]
            }
        })
        return result['result']['AddResults'][0]['Ids']

    @measured
    def get_bidmodifiers_by_campaign(self, campaign_id):
        """
        Выполняет GET-запрос за корректировками. Возвращает только общие для всех типов корректировок поля.
        """
        result = self.send_request(self.get_java_api_url() + '/json/v5/bidmodifiers/', {
            "method": "get",
            "params": {
                "SelectionCriteria": {
                    "CampaignIds": [campaign_id],
                    "Types": ["MOBILE_ADJUSTMENT", "DEMOGRAPHICS_ADJUSTMENT", "RETARGETING_ADJUSTMENT",
                              "REGIONAL_ADJUSTMENT"],
                    "Levels": ["CAMPAIGN", "AD_GROUP"]
                },
                "FieldNames": ["Id", "CampaignId", "AdGroupId", "Level", "Type"]
            }
        })
        return result['result']['BidModifiers']

    @measured
    def get_bidmodifiers(self, ids):
        """
        Выполняет GET-запрос за корректировками. Возвращает только общие для всех типов корректировок поля.
        """
        result = self.send_request(self.get_java_api_url() + '/json/v5/bidmodifiers/', {
            "method": "get",
            "params": {
                "SelectionCriteria": {
                    "Ids": ids,
                    "Types": ["MOBILE_ADJUSTMENT", "DEMOGRAPHICS_ADJUSTMENT", "RETARGETING_ADJUSTMENT",
                              "REGIONAL_ADJUSTMENT"],
                    "Levels": ["CAMPAIGN", "AD_GROUP"]
                },
                "FieldNames": ["Id", "CampaignId", "AdGroupId", "Level", "Type"]
            }
        })
        return result['result']['BidModifiers']

    @measured
    def set_bidmodifiers(self, ids):
        """
        Изменяет корректировки
        """
        result = self.send_request(self.get_java_api_url() + '/json/v5/bidmodifiers/', {
            "method": "set",
            "params": {
                "BidModifiers": [{"Id": _id, "BidModifier": random.randint(50, 1300)} for _id in ids]
            }
        })
        return result['result']['SetResults'][0]['Id']

    @measured
    def toggle_bidmodifiers(self, campaign_id=None, adgroup_id=None, bmtype="DEMOGRAPHICS_ADJUSTMENT", enabled=0):
        """
        Включает/выключает корректировки
        """
        result = self.send_request(self.get_java_api_url() + '/json/v5/bidmodifiers/', {
            "method": "toggle",
            "params": {
                "BidModifierToggleItems": [{
                    "CampaignId": campaign_id,
                    "AdGroupId": adgroup_id,
                    "Type": bmtype,
                    "Enabled": enabled
                }]
            }
        })
        return result['result']['ToggleResults'][0]

    @measured
    def delete_bidmodifiers(self, ids):
        """
        Удаляет корректировки по идентификаторам
        :param ids: список ID корректировок
        """
        self.send_request(self.get_old_api_url() + '/json/v5/bidmodifiers/', {
            "method": "delete",
            "params": {
                "SelectionCriteria": {
                    "Ids": ids
                }
            }
        })
