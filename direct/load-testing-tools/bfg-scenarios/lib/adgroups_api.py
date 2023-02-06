# coding=utf-8
import logging

from lib.base_api import BaseApi, measured_with

logging.basicConfig()
logger = logging.getLogger(__name__)
logger.setLevel('DEBUG')


class AdGroupsApi(BaseApi):
    def __init__(self, measurer, java_api_url, old_api_url, client_login, token, use_always_old_api=False):
        super(AdGroupsApi, self).__init__(measurer, java_api_url, old_api_url, client_login, token, use_always_old_api)

    @measured_with('pre_create_adgroup')
    def create_adgroup(self, campaign_id):
        """
        Создаёт новую текстовую группу и возвращает её ID
        """
        result = self.send_request(self.get_old_api_url() + '/json/v5/adgroups/', {
            "method": "add",
            "params": {
                "AdGroups": [{
                    "Name": "test_load",
                    "CampaignId": campaign_id,
                    "RegionIds": [0],
                    "NegativeKeywords": {
                        "Items": ["test"]
                    },
                    "TrackingParams": ""
                }]
            }
        })
        return result['result']['AddResults'][0]['Id']

    def delete_adgroup(self, adgroup_id):
        result = self.send_request(self.get_old_api_url() + '/json/v5/adgroups/', {
            "method": "delete",
            "params": {
                "SelectionCriteria": {
                    "Ids": [adgroup_id]
                }
            }
        })
        return result['result']['DeleteResults'][0]['Id']
