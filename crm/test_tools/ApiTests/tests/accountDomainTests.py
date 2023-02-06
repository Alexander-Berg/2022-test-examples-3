import requests
import json
import data.account
import data.properties
import data.accountDomain
#import pytest


def test_addDomainToAccount():
    newAccount = data.account.newAccount()
    addDomain = data.accountDomain.newAccountDomain(newAccount)
    success = addDomain['Success']
    assert success


def test_getDomains():
    newAccount = data.account.newAccount()
    addDomain = data.accountDomain.newAccountDomain(newAccount)
    getDomain= data.accountDomain.getAccountDomain(newAccount)
    domain = getDomain['domains'][0]['Domain']
    sourceId = getDomain['domains'][0]['SourceId']
    domainId = getDomain['domains'][0]['Id']
    assert domain == "Test Domain from Api"
    assert sourceId == 3
    assert domainId


def test_overwriteDomains():
    newAccount = data.account.newAccount()
    addDomain = data.accountDomain.newAccountDomain(newAccount)
    getDomain = data.accountDomain.getAccountDomain(newAccount)
    oldDomainId = getDomain['domains'][0]['Id']
    overwriteDomain = data.accountDomain.patchAccountDomains(newAccount)
    success = overwriteDomain['Success']
    assert success
    getDomain = data.accountDomain.getAccountDomain(newAccount)
    print(getDomain)
    domain = getDomain['domains'][0]['Domain']
    sourceId = getDomain['domains'][0]['SourceId']
    newDomainId = getDomain['domains'][0]['Id']
    assert domain == "Test Domain from Api patched"
    assert sourceId == 3
    assert oldDomainId != newDomainId


def test_deletingAccountDomains():
    newAccount = data.account.newAccount()
    addDomain = data.accountDomain.newAccountDomain(newAccount)
    getDomain = data.accountDomain.getAccountDomain(newAccount)
    oldDomainId = getDomain['domains'][0]['Id']
    deleteDomains = data.accountDomain.deleteAccountDomains(newAccount,oldDomainId)
    success = deleteDomains['Success']
    assert success
    getDomain = data.accountDomain.getAccountDomain(newAccount)
    domains = getDomain['domains']
    assert len(domains) == 0