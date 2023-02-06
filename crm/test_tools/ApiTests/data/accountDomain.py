import requests
import json
import data.properties
#import pytest

host = "https://crm-test.yandex-team.ru/api/v0/account/"
headers = data.properties.headers

newAccountDomainJson = {"domains": [{"domain": "Test Domain from Api","sourceId": "3"}]}
patchAccountDomainsJson = {"domains": [{"domain": "Test Domain from Api patched","sourceId": "3"}]}


def newAccountDomain(accountId):
    accountId = str(accountId)
    url = host +accountId +'/domains'
    print('Trying to add domains for account '+accountId)
    newDomain = requests.post(url, headers=headers, verify=False, json=newAccountDomainJson)
    response = newDomain.status_code
    assert response == 200
    newDomain = newDomain.json()
    print('Domain added successfully for account ' +accountId)
    return newDomain


def getAccountDomain(accountId):
    accountId = str(accountId)
    url = host +accountId +'/domains'
    print('Trying to get domains for account '+accountId)
    domains = requests.get(url,headers=headers,verify=False)
    response = domains.status_code
    assert response == 200
    domains = domains.json()
    print('Domains got successfully from account '+accountId)
    return domains


def patchAccountDomains(accountId):
    accountId = str(accountId)
    url = host +accountId +'/domains'
    print('Trying to get domains for account '+accountId)
    domains = requests.patch(url, headers=headers, verify=False, json=patchAccountDomainsJson)
    response = domains.status_code
    assert response == 200
    domains = domains.json()
    print('Domains overwrited successfully for account ' +accountId)
    return domains


def deleteAccountDomains(accountId, domainId):
    accountId = str(accountId)
    url = host +accountId +'/domains'
    print('Trying to delete domains for account '+accountId)
    deleteAccountDomainsJson = {"domains": [{"id": domainId}]}
    domains = requests.delete(url, headers=headers, verify=False, json=deleteAccountDomainsJson)
    response = domains.status_code
    assert response == 200
    domains = domains.json()
    print('Domains deleted successfully for account ' +accountId)
    return domains