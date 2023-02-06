import requests
import json
import data.properties
#import pytest

host = "https://crm-test.yandex-team.ru/api/v0/contact/"
headers = data.properties.headers

postContactJson = {"AccountId":"0","Kiks": [{"Email": "testfrom@api.com_test","Phone": "88009999999","PhoneExt": "5535","IsBroadcast": "false"}],"FirstName": "Ivan","MiddleName": "Fathername","LastName": "Ivanovich","Comment":"Test contact from API","BirthDate": "1986-06-06","LanguageId": "1","PositionId": "2"}
patchContactJson = {"FirstName": "Valera","MiddleName": "Pistoletov","LastName": "It is your time","Comment": "Contact from API patched","BirthDate": "2020-05-05","LanguageId": 2,"PositionId": 5}
postKikPhoneJson = {"Phone": "88005553535","PhoneExt": "444","IsBroadcast": "true"}
patchKikPhoneJson = {"Phone": "88009999999","PhoneExt": "333","IsBroadcast": "false"}
postKikMailJson = {"Email": "mail@fromapi.ru","IsBroadcast": "false"}
patchKikMailJson = {"Email": "patchmail@fromapi.ru","IsBroadcast": "true"}


def postContact(accountId):
    accountId = str(accountId)
    print('Trying to create new contact for account ' +accountId)
    postContactJson['AccountId'] = accountId
    newContact = requests.post(host, headers=headers, verify=False, json=postContactJson)
    response = newContact.status_code
    assert response == 200
    newContact = newContact.json()
    contactId = str(newContact['Id'])
    print('Contact ' +contactId +' created for account ' +accountId +' successfully')
    return newContact


def getContact(contactId):
    contactId = str(contactId)
    print('Trying to get contact ' +contactId)
    url = host + contactId
    contact = requests.get(url, headers=headers, verify=False)
    response = contact.status_code
    assert response == 200
    contact = contact.json()
    print('Contact ' +contactId +' got successfully')
    return contact


def patchContact(contactId):
    contactId = str(contactId)
    url = host + contactId
    print('Trying to patch contact ' +contactId)
    patchedContact = requests.patch(url, headers=headers, verify=False, json=patchContactJson)
    response = patchedContact.status_code
    assert response == 200
    patchedContact = patchedContact.json()
    print('Contact ' +contactId +' patched successfully')
    return patchedContact


def getKik(kikId):
    kikId = str(kikId)
    url = host + 'kik/' + kikId
    print('Trying to get kik, kikId '+kikId)
    kik = requests.get(url, headers=headers, verify=False)
    response = kik.status_code
    assert response == 200
    kik = kik.json()
    print('Kik ' +kikId +' got successfully')
    return kik


def postKikMail(contactId):
    contactId = str(contactId)
    print('Trying to create new kik mail for contact ' +contactId)
    url = host + contactId +'/kik'
    newKik = requests.post(url, headers=headers, verify=False, json=postKikMailJson)
    response = newKik.status_code
    assert response == 200
    newKik = newKik.json()
    kikId = str(newKik['Id'])
    print('New kik mail ' +kikId  +' for contact ' +contactId +' created successfully')
    return newKik


def postKikPhone(contactId):
    contactId = str(contactId)
    print('Trying to create new kik phone for contact ' +contactId)
    url = host + contactId +'/kik'
    newKik = requests.post(url, headers=headers, verify=False, json=postKikPhoneJson)
    response = newKik.status_code
    assert response == 200
    newKik = newKik.json()
    kikId = str(newKik['Id'])
    print('New kik phone ' +kikId  +' for contact ' +contactId +' created successfully')
    return newKik


def patchKikPhone(kikId):
    kikId = str(kikId)
    print('Trying to patch kik phone ' +kikId)
    url = host + 'kik/' + kikId
    patchedKik = requests.patch(url, headers=headers, verify=False, json=patchKikPhoneJson)
    response = patchedKik.status_code
    assert response == 200
    patchedKik = patchedKik.json()
    print('Kik phone '+kikId +' patched successfully')
    return patchedKik


def patchKikMail(kikId):
    kikId = str(kikId)
    print('Trying to patch kik mail ' +kikId)
    url = host + 'kik/' + kikId
    patchedKik = requests.patch(url, headers=headers, verify=False, json=patchKikMailJson)
    response = patchedKik.status_code
    assert response == 200
    patchedKik = patchedKik.json()
    print('Kik mail '+kikId +' patched successfully')
    return patchedKik


def deleteKik(contactId, kikId):
    contactId = str(contactId)
    kikId = str(kikId)
    print('Trying to delete kik ' +kikId +' from contact ' +contactId)
    url = host + contactId +'/kik/' +kikId
    deletedKik = requests.delete(url, headers=headers, verify=False)
    response = deletedKik.status_code
    assert response == 200
    print('Kik ' +kikId +' deleted from contact ' +contactId +' successfully')


def getDeletedKik(kikId):
    kikId = str(kikId)
    url = host + 'kik/' + kikId
    print('Trying to get deleted kik, kikId '+kikId)
    kik = requests.get(url, headers=headers, verify=False)
    response = kik.status_code
    assert response == 404
    kik = kik.json()
    print('Kik ' +kikId +' not found and deleted successfully')
    return kik['Code']


def getKiksfromAccount(accountId):
    accountId = str(accountId)
    url = host + 'account/' +accountId
    print('Trying to get kiks for account ' +accountId)
    kiks = requests.get(url, headers=headers, verify=False)
    response = kiks.status_code
    assert response == 200
    kiks = kiks.json()
    print('Kiks from account ' +accountId +' got successfully')
    return kiks