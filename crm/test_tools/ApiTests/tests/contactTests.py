import requests
import json
import data.account
import data.properties
import data.contact
#import pytest


def test_createNewContact():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    #print(newContact)
    firstName = newContact['FirstName']
    middleName = newContact['MiddleName']
    lastName = newContact['LastName']
    comment = newContact['Comment']
    languageId = newContact['LanguageId']
    positionId = newContact['PositionId']
    assert firstName == 'Ivan'
    assert middleName == 'Fathername'
    assert lastName == 'Ivanovich'
    assert comment == 'Test contact from API'
    assert languageId == 1
    assert positionId == 2


def test_getContact():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    getContact = data.contact.getContact(newContact)
    firstName = getContact['FirstName']
    middleName = getContact['MiddleName']
    lastName = getContact['LastName']
    comment = getContact['Comment']
    languageId = getContact['LanguageId']
    positionId = getContact['PositionId']
    assert firstName == 'Ivan'
    assert middleName == 'Fathername'
    assert lastName == 'Ivanovich'
    assert comment == 'Test contact from API'
    assert languageId == 1
    assert positionId == 2


def test_patchContact():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    patchedContact = data.contact.patchContact(newContact)
    firstName = patchedContact['FirstName']
    middleName = patchedContact['MiddleName']
    lastName = patchedContact['LastName']
    comment = patchedContact['Comment']
    languageId = patchedContact['LanguageId']
    positionId = patchedContact['PositionId']
    assert firstName == 'Valera'
    assert middleName == 'Pistoletov'
    assert lastName == 'It is your time'
    assert comment == 'Contact from API patched'
    assert languageId == 2
    assert positionId == 5


def test_newKikPhone():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    newKik = data.contact.postKikPhone(newContact)
    email = newKik['Email']
    phone = newKik['Phone']
    phoneExt = newKik['PhoneExt']
    broadcast = newKik['IsBroadcast']
    readonly = newKik['IsReadOnly']
    assert email is None
    assert phone == '88005553535'
    assert phoneExt == '444'
    assert broadcast
    assert readonly == 0


def test_newKikMail():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    newKik = data.contact.postKikMail(newContact)
    email = newKik['Email']
    phone = newKik['Phone']
    phoneExt = newKik['PhoneExt']
    broadcast = newKik['IsBroadcast']
    readonly = newKik['IsReadOnly']
    assert email == 'mail@fromapi.ru'
    assert phone is None
    assert phoneExt is None
    assert broadcast == 0
    assert readonly == 0

def test_getKikMail():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    newKik = data.contact.postKikMail(newContact)
    newKik = newKik['Id']
    getKik = data.contact.getKik(newKik)
    email = getKik['Email']
    phone = getKik['Phone']
    phoneExt = getKik['PhoneExt']
    broadcast = getKik['IsBroadcast']
    readonly = getKik['IsReadOnly']
    assert email == 'mail@fromapi.ru'
    assert phone is None
    assert phoneExt is None
    assert broadcast == 0
    assert readonly == 0


def test_getKikPhone():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    newKik = data.contact.postKikPhone(newContact)
    newKik = newKik['Id']
    getKik = data.contact.getKik(newKik)
    email = getKik['Email']
    phone = getKik['Phone']
    phoneExt = getKik['PhoneExt']
    broadcast = getKik['IsBroadcast']
    readonly = getKik['IsReadOnly']
    assert email is None
    assert phone == '88005553535'
    assert phoneExt == '444'
    assert broadcast
    assert readonly == 0


def test_patchKikMail():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    newKik = data.contact.postKikMail(newContact)
    newKik = newKik['Id']
    patchKik = data.contact.patchKikMail(newKik)
    email = patchKik['Email']
    phone = patchKik['Phone']
    phoneExt = patchKik['PhoneExt']
    broadcast = patchKik['IsBroadcast']
    readonly = patchKik['IsReadOnly']
    assert email == 'patchmail@fromapi.ru'
    assert phone is None
    assert phoneExt is None
    assert broadcast
    assert readonly == 0


def test_patchKikPhone():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    newKik = data.contact.postKikPhone(newContact)
    newKik = newKik['Id']
    patchKik = data.contact.patchKikPhone(newKik)
    email = patchKik['Email']
    phone = patchKik['Phone']
    phoneExt = patchKik['PhoneExt']
    broadcast = patchKik['IsBroadcast']
    readonly = patchKik['IsReadOnly']
    assert email is None
    assert phone == '88009999999'
    assert phoneExt == '333'
    assert broadcast == 0
    assert readonly == 0


def test_deleteKik():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    newContact = newContact['Id']
    newKik = data.contact.postKikPhone(newContact)
    newKik = newKik['Id']
    deleteKik = data.contact.deleteKik(newContact,newKik)
    getDeletedKik = data.contact.getDeletedKik(newKik)
    assert getDeletedKik == 'NOT_FOUND'


def test_getKiksFromAccount():
    newAccount = data.account.newAccount()
    newContact = data.contact.postContact(newAccount)
    kik1 = newContact['AccountKiks'][0]['Id']
    kik2 = newContact['AccountKiks'][1]['Id']
    newContact = newContact['Id']
    kik3 = data.contact.postKikPhone(newContact)
    kik3 = kik3['Id']
    kik4 = data.contact.postKikMail(newContact)
    kik4 = kik4['Id']
    oldKikList = [kik1,kik2,kik3,kik4]
    oldKikList.sort()
    getKiks = data.contact.getKiksfromAccount(newAccount)
    accountKik1 = getKiks[0]['AccountKiks'][0]['Id']
    accountKik2 = getKiks[0]['AccountKiks'][1]['Id']
    accountKik3 = getKiks[0]['AccountKiks'][2]['Id']
    accountKik4 = getKiks[0]['AccountKiks'][3]['Id']
    accountKikList = [accountKik1,accountKik2,accountKik3,accountKik4]
    accountKikList.sort()
    assert oldKikList == accountKikList

