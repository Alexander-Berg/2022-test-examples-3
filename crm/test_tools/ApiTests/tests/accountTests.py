import requests
import json
import data.account
import data.properties
#import pytest


def test_getAccount():
    account = data.account.getAccount(9955466)
    clientId = account['ClientId']
    login = account['Login']
    assert clientId == 4676937
    assert login == 'yndx-agroroza'

def test_creatingAccount():
    account = data.account.newAccount()
    createdAccount = data.account.getAccount(account)
    accountTypeId = createdAccount['AccountTypeId']
    accountId = createdAccount['Id']
    accountName = createdAccount['Name']
    assert accountTypeId == 8
    assert accountId == account
    assert accountName == 'Test con from API'


def test_patchingAccount():
    account = data.account.newAccount()
    changedAccount = data.account.patchAccount(account)
    changedAccountName = changedAccount['Name']
    changedAccountId = changedAccount['Id']
    assert changedAccountName == 'Test con from API patched'
    assert changedAccountId == account


def test_getAccountByClientId():
    account = data.account.getAccountByClientId(4676937)
    clientId = account['ClientId']
    login = account['Login']
    id = account['Id']
    assert clientId == 4676937
    assert login == 'yndx-agroroza'
    assert id == 9955466


def test_getAccountByLogin():
    account = data.account.getAccountByLogin('yndx-agroroza')
    clientId = account['ClientId']
    login = account['Login']
    id = account['Id']
    assert clientId == 4676937
    assert login == 'yndx-agroroza'
    assert id == 9955466


def test_ArchivingAccount():
    newAccount = data.account.newAccount()
    archivedAccount = data.account.makeArchived(newAccount)
    success = archivedAccount['Success']
    assert success
    getAccount = data.account.getArchivedAccount(newAccount)
    status = getAccount['Code']
    assert status == 'INVALID_PARAMS'