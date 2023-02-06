# coding=utf-8
import logging
from datetime import datetime

from lib.base_api import BaseApi, measured_with

logging.basicConfig()
logger = logging.getLogger(__name__)
logger.setLevel('DEBUG')


class CampaignsApi(BaseApi):
    def __init__(self, measurer, java_api_url, old_api_url, client_login, token, use_always_old_api=False):
        super(CampaignsApi, self).__init__(measurer, java_api_url, old_api_url, client_login, token, use_always_old_api)

    @measured_with('pre_create_campaign')
    def create_campaign(self):
        """
        Создаёт новую текстовую кампанию и возвращает её ID
        """
        result = self.send_request(self.get_old_api_url() + '/json/v5/campaigns/', {
            "method": "add",
            "params": {
                "Campaigns": [
                    {
                        "Name": "test_load",
                        "StartDate": datetime.today().strftime("%Y-%m-%d"),
                        "TextCampaign": {
                            "BiddingStrategy": {
                                "Search": {
                                    "WbMaximumClicks": {
                                        "WeeklySpendLimit": 4000000000
                                    },
                                    "BiddingStrategyType": "WB_MAXIMUM_CLICKS"
                                },
                                "Network": {
                                    "BiddingStrategyType": "NETWORK_DEFAULT",
                                    "NetworkDefault": {
                                    }
                                }
                            }
                        }
                    }
                ]
            }
        })
        return result['result']['AddResults'][0]['Id']

    def delete_campaign(self, campaign_id):
        result = self.send_request(self.get_old_api_url() + '/json/v5/campaigns/', {
            "method": "delete",
            "params": {
                "SelectionCriteria": {
                    "Ids": [campaign_id]
                }
            }
        })
        return result['result']['DeleteResults'][0]['Id']
