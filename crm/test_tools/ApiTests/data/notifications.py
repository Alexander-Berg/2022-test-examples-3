import requests
import json
import data.properties
#import pytest

host = "https://crm-test.yandex-team.ru/api/v0/notification/"
headers = data.properties.headers

postNotificationJson = {"subject": "New notification from API","body": "Test notification from API","addressee": {"Login":"agroroza"}}


def postNotification():
    print('Trying to create new notification')
    newNotification = requests.post(host,headers=headers, verify=False, json=postNotificationJson)
    response = newNotification.status_code
    assert response == 200
    newNotification = newNotification.json()
    print('Notification created successfully')
    return newNotification
