import requests
import json
import data.properties
#import pytest

host = "https://crm-test.yandex-team.ru/api/v0/account/"
headers = data.properties.headers

postAccountJson = {"AccountTypeId": "8","Name": "Test con from API","Domain": "yandex.ru","OrganizationId": "3","IsVip": "true","IsPseudoagency": "true","WelcomeZone": "true","CityId":"114224" }
patchAccountJson = {"Name": "Test con from API patched" }


def getAccount(accountId):
    accountId = str(accountId)
    url = host + accountId
    print('Try to get account ' +accountId)
    account = requests.get(url,headers=headers, verify=False)
    response = account.status_code
    assert response == 200
    account = account.json()
    print('Account got successfully')
    return account


def getArchivedAccount(accountId):
    accountId = str(accountId)
    url = host + accountId
    print('Try to get account ' +accountId)
    account = requests.get(url,headers=headers, verify=False)
    response = account.status_code
    assert response == 400
    account = account.json()
    print('Archived account got successfully')
    return account


def newAccount():
    print('Try to create new account')
    newAccount = requests.post(host,headers=headers,verify=False,json=postAccountJson)
    newAccount = newAccount.json()
    newAccountId = newAccount['Id']
    print('Account created successfully')
    return newAccountId


def patchAccount(accountId):
    accountId=str(accountId)
    url = host + accountId
    print('Trying to patch account ' +accountId)
    patchedAccount = requests.patch(url,headers=headers,verify=False,json=patchAccountJson)
    patchedAccount = patchedAccount.json()
    print('Account patched successfully')
    return patchedAccount


def getAccountByClientId(id):
    id = str(id)
    url = host + 'byclientId/' +id
    print('Trying to get account by clientId ' +id)
    account = requests.get(url, headers=headers,verify=False)
    response = account.status_code
    assert response == 200
    account = account.json()
    print('Account got successfully for clientId ' +id)
    return account


def getAccountByLogin(login):
    login=str(login)
    print('Trying to get account by login ' +login)
    url = host + 'bylogin/' +login
    account = requests.get(url,headers=headers,verify=False)
    response = account.status_code
    assert response == 200
    account = account.json()
    print('Account got successfully for login ' +login)
    return account


def makeArchived(accountId):
    accountId = str(accountId)
    print('Trying to make account ' +accountId +' archived')
    url = host + accountId + '/makeArchived'
    archivedAccount = requests.post(url,headers=headers,verify=False)
    response = archivedAccount.status_code
    assert response == 200
    archivedAccount = archivedAccount.json()
    print('Account '+accountId+' archived successfully')
    return archivedAccount
