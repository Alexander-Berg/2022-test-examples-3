import requests
import json
import data.notifications
import data.properties
#import pytest

def test_newNotification():
    newNotification = data.notifications.postNotification()
    notificationId = newNotification['id']
    assert notificationId
