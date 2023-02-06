import pytest
import vobject

from hamcrest import assert_that, equal_to

from mail.sheltie.package.python.transform import (
    makeFullName,
    toVcardNames,
    toVcardEmails,
    toVcardPhones,
    toVcardNotes,
    toVcardEvents,
    toVcardOrganizations,
    toVcardNickNames,
    toVcardAddresses,
    toVcardWebSites,
    toVcardUids,
    toVcardSocialProfile,
    toVcardInstantMessengers,
    toVcardTimeZones,
    toVcard,
    transformToVcard,
    fromVcardNames,
    fromVcardEmails,
    fromVcardPhones,
    fromVcardNotes,
    parseDate,
    fromVcardEvents,
    fromVcardOrganizations,
    fromVcardNickNames,
    fromVcardAddresses,
    fromVcardWebSites,
    fromVcardUids,
    fromVcardSocialProfile,
    fromVcardInstantMessengers,
    fromVcardAdditionalSocialProfile,
    fromVcardAdditionalInstantMessengers,
    fromVcardTimeZones,
    fromVcard,
    transformFromVcard,
    vcardEncoder,
    exportContacts,
    vcardDecoder,
    importContacts
)


def test_hist():
    assert_that(1, equal_to(1))


@pytest.mark.parametrize(('name', 'expected'), (
    ({'prefix': 'prefix', 'first': 'first', 'middle': 'middle', 'last': 'last', 'suffix': 'suffix'},
        'prefix first middle last suffix'),
    ({'prefix': 'prefix', 'first': 'first'}, 'prefix first'),
    ({'prefix': 'prefix', 'middle': 'middle', 'suffix': 'suffix'}, 'prefix middle suffix'),
    ({}, '')
))
def testMakeFullName(name, expected):
    assert_that(makeFullName(name), equal_to(expected))


@pytest.mark.parametrize(('contact', 'prefix', 'first', 'middle', 'last', 'suffix', 'fullName'), (
    ({'names': [{'prefix': 'prefix', 'first': 'first', 'middle': 'middle', 'last': 'last', 'suffix': 'suffix'}]},
        'prefix', 'first', 'middle', 'last', 'suffix', 'prefix first middle last suffix'),
    ({'names': [{'prefix': 'prefix', 'first': 'first'}]}, 'prefix', 'first', '', '', '', 'prefix first'),
    ({'names': [{'prefix': 'prefix', 'middle': 'middle', 'suffix': 'suffix'}], 'emails': []},
        'prefix', '', 'middle', '', 'suffix', 'prefix middle suffix'),
    ({'names': []}, '', '', '', '', '', ''),
    ({'names': [{'prefix': 'prefix', 'middle': 'middle', 'suffix': 'suffix'}, {'prefix': 'pr', 'middle': 'mi', 'suffix': 'su'}], 'emails': []},
        'prefix', '', 'middle', '', 'suffix', 'prefix middle suffix'),
    ({'names': [{}]}, '', '', '', '', '', ''),
))
def testToVcardNames(contact, prefix, first, middle, last, suffix, fullName):
    vobj = vobject.vCard()
    toVcardNames(contact, vobj)
    assert_that(vobj.fn.value, equal_to(fullName))
    assert_that(vobj.n.value.family, equal_to(last))
    assert_that(vobj.n.value.given, equal_to(first))
    assert_that(vobj.n.value.additional, equal_to(middle))
    assert_that(vobj.n.value.prefix, equal_to(prefix))
    assert_that(vobj.n.value.suffix, equal_to(suffix))


@pytest.mark.parametrize(('contact', 'email', 'paramType', 'exisEmail', 'exisParamType'), (
    ({'names': [{'prefix': 'prefix'}], 'emails': [{'email': 'email', 'type': ['WORK']}]}, ['email'], ['WORK'], True, [True]),
    ({'names': [{'prefix': 'prefix'}], 'emails': [{'email': 'email', 'type': ['WORK', 'heh']}]}, ['email'], ['WORK'], True, [True]),
    ({'names': [{'prefix': 'prefix'}], 'emails': [{'email': 'email', 'type': ['WORK', 'HOME']}]}, ['email'], ['HOME'], True, [True]),
    ({'names': [{'prefix': 'prefix'}], 'emails': [{'email': 'email1', 'type': ['WORK', 'HOME']}, {'email': 'email2', 'type': ['HOME', 'WORK']}]},
        ['email1', 'email2'], ['HOME', 'WORK'], True, [True, True]),
    ({'names': [{'prefix': 'prefix'}], 'emails': [{'email': 'email', 'type': []}]}, ['email'], [], True, [False]),
    ({'names': [{'prefix': 'prefix'}], 'emails': [{'email': 'email'}]}, ['email'], [], True, [False]),
    ({'emails': [{'email': 'email1'}, {'email': 'email2', 'type': ['WORK']}]}, ['email1', 'email2'], [None, 'WORK'], True, [False, True]),
    ({'names': [{'prefix': 'prefix'}], 'emails': []}, [], [], False, [False]),
    ({'names': [{'prefix': 'prefix'}], 'emails': [{}]}, [], [], False, [False]),
))
def testToVcardEmails(contact, email, paramType, exisEmail, exisParamType):
    vobj = vobject.vCard()
    toVcardEmails(contact, vobj)
    if exisEmail:
        assert_that(vobj.email.value, equal_to(email[0]))
        for i in range(len(vobj.email_list)):
            assert_that(vobj.email_list[i].value, equal_to(email[i]))
            if exisParamType[i]:
                assert_that(vobj.email_list[i].type_param, equal_to(paramType[i]))
    else:
        assert_that(hasattr(vobj, 'email'), equal_to(exisEmail))


@pytest.mark.parametrize(('contact', 'telephone_number', 'paramType', 'exisTel', 'exisParamType'), (
    ({'telephone_numbers': [{'telephone_number': 'telephone_number', 'type': ['WORK']}]}, ['telephone_number'], ['WORK'], True, [True]),
    ({'telephone_numbers': [{'telephone_number': 'telephone_number', 'type': ['WORK', 'heh']}]}, ['telephone_number'], ['WORK'], True, [True]),
    ({'telephone_numbers': [{'telephone_number': 'telephone_number', 'type': ['WORK', 'HOME']}]}, ['telephone_number'], ['HOME'], True, [True]),
    ({'telephone_numbers':
        [{'telephone_number': 'telephone_number1', 'type': ['WORK', 'HOME']}, {'telephone_number': 'telephone_number2', 'type': ['HOME', 'WORK']}]},
        ['telephone_number1', 'telephone_number2'], ['HOME', 'WORK'], True, [True, True]),
    ({'telephone_numbers': [{'telephone_number': 'telephone_number', 'type': []}]}, ['telephone_number'], [], True, [False]),
    ({'telephone_numbers': [{'telephone_number': 'telephone_number'}]}, ['telephone_number'], [], True, [False]),
    ({'telephone_numbers': [{'telephone_number': 'telephone_number1'}, {'telephone_number': 'telephone_number2', 'type': ['WORK']}]},
        ['telephone_number1', 'telephone_number2'], [None, 'WORK'], True, [False, True]),
    ({'telephone_numbers': []}, [], [], False, [False]),
    ({'telephone_numbers': [{}]}, [], [], False, [False]),
))
def testToVcardPhones(contact, telephone_number, paramType, exisTel, exisParamType):
    vobj = vobject.vCard()
    toVcardPhones(contact, vobj)
    if exisTel:
        assert_that(vobj.tel.value, equal_to(telephone_number[0]))
        for i in range(len(vobj.tel_list)):
            assert_that(vobj.tel_list[i].value, equal_to(telephone_number[i]))
            if exisParamType[i]:
                assert_that(vobj.tel_list[i].type_param, equal_to(paramType[i]))
    else:
        assert_that(hasattr(vobj, 'tel'), equal_to(exisTel))


@pytest.mark.parametrize(('contact', 'notes', 'exisNote'), (
    ({'names': [{'prefix': 'prefix'}], 'notes': ['1', '2', '3']}, ['1', '2', '3'], True),
    ({'names': [{'prefix': 'prefix'}], 'notes': ['1', '2', '3'], 'description': 'description'}, ['1', '2', '3', 'description'], True),
    ({'names': [{'prefix': 'prefix'}], 'notes': ['1', '2', '3'], 'description': '1'}, ['1', '2', '3'], True),
    ({'names': [{'prefix': 'prefix'}], 'description': '1'}, ['1'], True),
    ({'names': [{'prefix': 'prefix'}], 'notes': []}, [], False),
))
def testToVcardNotes(contact, notes, exisNote):
    vobj = vobject.vCard()
    toVcardNotes(contact, vobj)
    if exisNote:
        assert_that(vobj.note.value, equal_to(notes[0]))
        for i in range(len(vobj.note_list)):
            assert_that(vobj.note_list[i].value, equal_to(notes[i]))
    else:
        assert_that(hasattr(vobj, 'note'), equal_to(exisNote))


@pytest.mark.parametrize(('contact', 'dates', 'exisBDay',), (
    ({'events': [{'a': [], 'type': ['birthday'], 'day': 1, 'month': 2}]}, ['--0201'], True),
    ({'events': [{'a': [], 'type': ['birthday'], 'day': 1, 'month': 2, 'year': 1111}]}, ['11110201'], True),
    ({'events': [{'a': [], 'type': ['birthday', 'heh'], 'day': 1, 'month': 2}]}, ['--0201'], True),
    ({'events': [{'a': [], 'type': ['heh']}]}, [], False),
    ({'events': [{'a': [], 'type': ['birthday', 'heh'], 'day': 1, 'month': 2}, {'a': [], 'type': ['birthday', 'hah'], 'day': 3, 'month': 4}]},
        ['--0201', '--0403'], True),
    ({'events': [{'a': [], 'type': []}]}, [], False),
    ({'events': [{'a': []}]}, [], False),
    ({'events': [{'a': []}, {'a': [], 'type': ['birthday'], 'day': 1, 'month': 2}]}, ['--0201'], True),
    ({'events': []}, [], False),
    ({'events': [{'a': [], 'type': ['birthday'], 'year': 1111}]}, ['11110000'], True),
    ({'events': [{}]}, [], False),
))
def testToVcardEvents(contact, dates, exisBDay):
    vobj = vobject.vCard()
    toVcardEvents(contact, vobj)
    if exisBDay:
        assert_that(vobj.bday.value, equal_to(dates[0]))
        for i in range(len(vobj.bday_list)):
            assert_that(vobj.bday_list[i].value, equal_to(dates[i]))
    else:
        assert_that(hasattr(vobj, 'bday'), equal_to(exisBDay))


@pytest.mark.parametrize(('contact', 'orgs', 'titles', 'exisOrg', 'exisTitle'), (
    ({'organizations': [{'company': 'company', 'department': 'department', 'title': 'title', 'summary': 'summary'}]},
        [['company', 'department', 'summary', 'title']], ['title'], True, True),
    ({'organizations': [{'company': 'company', 'title': 'title'}]}, [['company', '', '', 'title']],
        ['title'], True, True),
    ({'organizations': [{'company': 'company', 'department': 'department'}]},
        [['company', 'department', '', '']], ['title'], True, False),
    ({'organizations': [{'company': 'company1', 'department': 'department1', 'title': 'title1'}, {'company': 'company2'}]},
        [['company1', 'department1', '', 'title1'], ['company2', '', '', '']], ['title1'], True, True),
    ({'organizations': [{'title': 'title'}]}, [['', '', '', '']], ['title'], False, True),
    ({'organizations': [{'company': 'company1', 'department': 'department1', 'title': 'title1'}, {'title': 'title2'}]},
        [['company1', 'department1', '', 'title1'], ['', '', '', 'title2']], ['title1', 'title2'], True, True),
    ({'organizations': [{}]}, [], [], False, False),
))
def testToVcardOrganizations(contact, orgs, titles, exisOrg, exisTitle):
    vobj = vobject.vCard()
    toVcardOrganizations(contact, vobj)
    if exisOrg:
        assert_that(vobj.org.value, equal_to(orgs[0]))
        for i in range(len(vobj.org_list)):
            assert_that(vobj.org_list[i].value, equal_to(orgs[i]))
    else:
        assert_that(hasattr(vobj, 'org'), equal_to(exisOrg))
    if exisTitle:
        assert_that(vobj.title.value, equal_to(titles[0]))
        for i in range(len(vobj.title_list)):
            assert_that(vobj.title_list[i].value, equal_to(titles[i]))
    else:
        assert_that(hasattr(vobj, 'title'), equal_to(exisTitle))


@pytest.mark.parametrize(('contact', 'nickNames', 'exisNickName'), (
    ({'names': [{'prefix': 'prefix'}], 'nicknames': ['1', '2', '3']}, ['1', '2', '3'], True),
    ({'names': [{'prefix': 'prefix'}], 'nicknames': []}, [], False),
))
def testToVcardNickNames(contact, nickNames, exisNickName):
    vobj = vobject.vCard()
    toVcardNickNames(contact, vobj)
    if exisNickName:
        assert_that(vobj.nickname.value, equal_to(nickNames[0]))
        for i in range(len(vobj.nickname_list)):
            assert_that(vobj.nickname_list[i].value, equal_to(nickNames[i]))
    else:
        assert_that(hasattr(vobj, 'nickname'), equal_to(exisNickName))


@pytest.mark.parametrize(('contact', 'streets', 'cities', 'paramType', 'exisAdr', 'exisParamType'), (
    ({'addresses': [{'street': 'street', 'city': 'city', 'type': ['WORK']}]}, ['street'], ['city'], ['WORK'], True, [True]),
    ({'addresses': [{'street': 'street', 'type': ['WORK', 'heh']}]}, ['street'], [''], ['WORK'], True, [True]),
    ({'addresses': [{'street': 'street', 'type': ['WORK', 'HOME']}]}, ['street'], [''], ['HOME'], True, [True]),
    ({'addresses': [{'street': 'street1', 'type': ['WORK', 'HOME']}, {'street': 'street2', 'city': 'city2', 'type': ['HOME', 'WORK']}]},
        ['street1', 'street2'], ['', 'city2'], ['HOME', 'WORK'], True, [True, True]),
    ({'addresses': [{'street': 'street', 'type': []}]}, ['street'], [''], [], True, [False]),
    ({'addresses': [{'street': 'street'}]}, ['street'], [''], [], True, [False]),
    ({'addresses': [{'street': 'street1'}, {'street': 'street2', 'type': ['WORK']}]},
        ['street1', 'street2'], ['', ''], [None, 'WORK'], True, [False, True]),
    ({'addresses': []}, [], [], [], False, [False]),
    ({'addresses': [{}]}, [''], [''], [None], True, [False]),
))
def testToVcardAddresses(contact, streets, cities, paramType, exisAdr, exisParamType):
    vobj = vobject.vCard()
    toVcardAddresses(contact, vobj)
    if exisAdr:
        assert_that(vobj.adr.value.street, equal_to(streets[0]))
        assert_that(vobj.adr.value.city, equal_to(cities[0]))
        for i in range(len(vobj.adr_list)):
            assert_that(vobj.adr_list[i].value.street, equal_to(streets[i]))
            assert_that(vobj.adr_list[i].value.city, equal_to(cities[i]))
            if exisParamType[i]:
                assert_that(vobj.adr_list[i].type_param, equal_to(paramType[i]))
    else:
        assert_that(hasattr(vobj, 'adr'), equal_to(exisAdr))


@pytest.mark.parametrize(('contact', 'urls', 'exisUrl'), (
    ({'websites': [{'a': '', 'url': '1'}]}, ['1'], True),
    ({'websites': [{'a': '', 'url': '1'}, {'url': '2'}]}, ['1', '2'], True),
    ({'websites': []}, [], False),
    ({'websites': [{}]}, [], False),
))
def testToVcardWebSites(contact, urls, exisUrl):
    vobj = vobject.vCard()
    toVcardWebSites(contact, vobj)
    if exisUrl:
        assert_that(vobj.url.value, equal_to(urls[0]))
        for i in range(len(vobj.url_list)):
            assert_that(vobj.url_list[i].value, equal_to(urls[i]))
    else:
        assert_that(hasattr(vobj, 'url'), equal_to(exisUrl))


@pytest.mark.parametrize(('contact', 'uids', 'exisUid'), (
    ({'vcard_uids': ['1', '2', '3']}, ['1', '2', '3'], True),
    ({'vcard_uids': []}, [], False),
))
def testToVcardUids(contact, uids, exisUid):
    vobj = vobject.vCard()
    toVcardUids(contact, vobj)
    if exisUid:
        assert_that(vobj.uid.value, equal_to(uids[0]))
        for i in range(len(vobj.uid_list)):
            assert_that(vobj.uid_list[i].value, equal_to(uids[i]))
    else:
        assert_that(hasattr(vobj, 'uid'), equal_to(exisUid))


@pytest.mark.parametrize(('contact', 'social_profiles', 'paramType', 'exisXSocialProfile', 'exisParamType'), (
    ({'social_profiles': [{'profile': 'profile', 'type': ['WORK']}]}, ['profile'], ['WORK'], True, [True]),
    ({'social_profiles': [{'profile': 'profile', 'type': ['WORK', 'heh']}]}, ['profile'], ['WORK'], True, [True]),
    ({'social_profiles': [{'profile': 'profile1', 'type': ['WORK', 'HOME']}, {'profile': 'profile2', 'type': ['HOME', 'WORK']}]},
        ['profile1', 'profile2'], ['WORK', 'HOME'], True, [True, True]),
    ({'social_profiles': [{'profile': 'profile', 'type': []}]}, ['profile'], [], True, [False]),
    ({'social_profiles': [{'profile': 'profile'}]}, [''], [], True, [False]),
    ({'social_profiles': [{'profile': 'profile1'}, {'profile': 'profile2', 'type': ['WORK']}]}, ['', 'profile2'], [None, 'WORK'], True, [False, True]),
    ({'social_profiles': []}, [], [], False, [False]),
    ({'social_profiles': [{'type': ['WORK']}]}, [''], ['WORK'], True, [True]),
))
def testToVcardSocialProfile(contact, social_profiles, paramType, exisXSocialProfile, exisParamType):
    vobj = vobject.vCard()
    toVcardSocialProfile(contact, vobj)
    if exisXSocialProfile:
        assert_that(vobj.x_socialprofile.value, equal_to(social_profiles[0]))
        for i in range(len(vobj.x_socialprofile_list)):
            assert_that(vobj.x_socialprofile_list[i].value, equal_to(social_profiles[i]))
            if exisParamType[i]:
                assert_that(vobj.x_socialprofile_list[i].type_param, equal_to(paramType[i]))
    else:
        assert_that(hasattr(vobj, 'x_socialprofile'), equal_to(exisXSocialProfile))


@pytest.mark.parametrize(('contact', 'messengers', 'paramType', 'exisImpp', 'exisParamType'), (
    ({'instant_messengers': [{'a': ''}]}, [], [], False, False),
    ({'instant_messengers': [{'service_id': 'service_id'}, {'a': ''}]}, ['service_id'], [], True, [False]),
    ({'instant_messengers': [{'service_id': 'service_id1'}, {'a': ''}, {'service_id': 'service_id2'}]}, ['service_id1', 'service_id2'], [], True, [False, False]),
    ({'instant_messengers': [{'service_id': 'service_id', 'service_type': 'service_type'}]}, ['service_id'], [['service_type']], True, [True]),
    ({'instant_messengers': [{'service_id': 'service_id', 'protocol': 'protocol', 'service_type': 'service_type'}]}, ['protocol:service_id'], [['service_type']], True, [True]),
    ({'instant_messengers': [{'service_id': 'service_id1', 'protocol': 'protocol1'}, {'service_id': 'service_id2', 'protocol': 'protocol2', 'service_type': 'service_type2'}]},
        ['protocol1:service_id1', 'protocol2:service_id2'], [None, ['service_type2']], True, [False, True]),
    ({'instant_messengers': []}, [], [], False, [False]),
    ({'instant_messengers': [{}]}, [], [], False, [False]),
))
def testToVcardInstantMessengers(contact, messengers, paramType, exisImpp, exisParamType):
    vobj = vobject.vCard()
    toVcardInstantMessengers(contact, vobj)
    if exisImpp:
        assert_that(vobj.impp.value, equal_to(messengers[0]))
        for i in range(len(vobj.impp_list)):
            assert_that(vobj.impp_list[i].value, equal_to(messengers[i]))
            if exisParamType[i]:
                assert_that(vobj.impp_list[i].params['x-service-type'], equal_to(paramType[i]))
    else:
        assert_that(hasattr(vobj, 'impp'), equal_to(exisImpp))


@pytest.mark.parametrize(('contact', 'tzs', 'exisTZ'), (
    ({'names': [{'prefix': 'prefix'}], 'tzs': ['1', '2', '3']}, ['1', '2', '3'], True),
    ({'names': [{'prefix': 'prefix'}], 'tzs': []}, [], False),
))
def testToVcardTimeZones(contact, tzs, exisTZ):
    vobj = vobject.vCard()
    toVcardTimeZones(contact, vobj)
    if exisTZ:
        assert_that(vobj.tz.value, equal_to(tzs[0]))
        for i in range(len(vobj.tz_list)):
            assert_that(vobj.tz_list[i].value, equal_to(tzs[i]))
    else:
        assert_that(hasattr(vobj, 'tz'), equal_to(exisTZ))


@pytest.mark.parametrize(('contact', 'expected'), (
    ({}, 'BEGIN:VCARD\r\nVERSION:3.0\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n'),
    ({'names': []}, 'BEGIN:VCARD\r\nVERSION:3.0\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n'),
    ({'names': [{'prefix': 'prefix', 'first': 'first', 'middle': 'middle', 'last': 'last', 'suffix': 'suffix'}]},
        'BEGIN:VCARD\r\nVERSION:3.0\r\nFN:prefix first middle last suffix\r\nN:last;first;middle;prefix;suffix\r\nEND:VCARD\r\n'),
    ({'emails': [{'email': 'email1', 'type': ['WORK', 'HOME']}, {'email': 'email2', 'type': ['HOME', 'WORK']}]},
        'BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL;TYPE=HOME:email1\r\nEMAIL;TYPE=WORK:email2\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n'),
    ({'emails': [{'email': 'email1', 'type': ['WORK', 'HOME']}, {'email': 'email2', 'type': ['HOME', 'WORK']}], 'nicknames': ['1', '2', '3']},
        'BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL;TYPE=HOME:email1\r\nEMAIL;TYPE=WORK:email2\r\nFN:\r\nN:;;;;\r\nNICKNAME:1\r\nNICKNAME:2\r\nNICKNAME:3\r\nEND:VCARD\r\n'),
))
def testToVcard(contact, expected):
    assert_that(toVcard(contact), equal_to(expected))


@pytest.mark.parametrize(('uid', 'contactsStr', 'expected'), (
    ('1', r'{}', r'{}'),
    ('1', r'{"1": {"emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}]}}',
        r'{"1": "BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:\r\nN:;;;;\r\nTEL:8800\r\nEND:VCARD\r\n"}'),
    ('1', r'{"1": {"names": [{"first": "Eric", "last": "Cartman"}], "emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}]}}',
        r'{"1": "BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n"}'),
    ('1', r'{"1": {"names": [{"first": "Eric"}], "emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}]},'
        r'"2": {"names": [{"first": "Kenny"}], "emails": [{"email": "kenny@.alive.ru"}], "vcard_uids": ["14", "08"], "tzs": ["Europe/Moscow"]}}',
        r'{"1": "BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric\r\nN:;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n", '
        r'"2": "BEGIN:VCARD\r\nVERSION:3.0\r\nUID:14\r\nUID:08\r\nEMAIL:kenny@.alive.ru\r\nFN:Kenny\r\nN:;Kenny;;;\r\nTZ:Europe/Moscow\r\nEND:VCARD\r\n"}'),
    ('1', r'{"1": {"emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}], "notes": ["1", "2", "3"], '
            r'"events": [{"type": ["birthday"], "day": 1}, {"type": ["anniversary"], "day": 1, "month": 2, "year": 1111}, {"type": ["evolution-anniversary"], "day": 1, "month": 2}], '
            r'"organizations": [{"company": "company", "department": "department", "title": "title", "type": ["type1", "type2"]}], "jabbers": [{"type": "WORK", "jabber": "first@ya.ru"}, '
            r'{"jabber": "second@ya.ru"}], "nicknames": ["1", "2", "3"], "addresses": [{"street": "street", "city": "city", "type": ["WORK"]}], "websites": [{"url": "1"}], '
            r'"social_profiles": [{"profile": "profile", "type": ["WORK"]}, {"profile": "twitter", "type": ["twitter", "HOME"]}, {"profile": "jabber2@yandex.ru", '
            r'"type": ["jabber", "WORK", "HOME"]}], "instant_messengers": [{"service_id": "service_id", "protocol": "protocol", "service_type": "service_type", "type": ["HOME"]}, '
            r'{"service_id": "skype1", "service_type": "skype", "type": ["HOME", "work"]}], "vcard_uids": ["1", "2", "3"]}}',
        r'{"1": "BEGIN:VCARD\r\nVERSION:3.0\r\nUID:1\r\nUID:2\r\nUID:3\r\nADR;TYPE=WORK:;;street;city;;;\r\nBDAY:00000001\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:\r\n'
        r'IMPP;TYPE=HOME;x-service-type=service_type:protocol:service_id\r\nIMPP;TYPE=HOME,work;x-service-type=skype:skype1\r\nN:;;;;\r\nNICKNAME:1\r\nNICKNAME:2\r\nNICKNAME:3\r\nNOTE:1\r\nNOTE:2\r\n'
        r'NOTE:3\r\nORG;TYPE=type1,type2:company;department;;title\r\nTEL:8800\r\nTITLE:title\r\nURL:1\r\nX-ANNIVERSARY:11110201\r\nX-EVOLUTION-ANNIVERSARY:--0201\r\n'
        r'X-JABBER;TYPE=WORK,HOME:jabber2@yandex.ru\r\nX-SKYPE;TYPE=HOME,work:skype1\r\nX-SOCIALPROFILE;TYPE=WORK:profile\r\nX-SOCIALPROFILE;TYPE=twitter:twitter\r\n'
        r'X-SOCIALPROFILE;TYPE=jabber:jabber2@yandex.ru\r\nX-TWITTER;TYPE=HOME:twitter\r\nEND:VCARD\r\n"}'),
))
def testTransformToVcard(uid, contactsStr, expected):
    assert_that(transformToVcard(uid, contactsStr), equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'a': 'b'}, {'a': 'b', 'names': [{'middle': '', 'prefix': '', 'last': 'Cartman', 'suffix': '', 'first': 'Eric'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'a': 'b'}, {'a': 'b', 'names': [{'middle': '', 'prefix': '', 'last': 'Cartman', 'suffix': '', 'first': 'Eric'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:fn\r\nN:last;first;middle;prefix;suffix\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'a': 'b'}, {'a': 'b', 'names': [{'middle': 'middle', 'prefix': 'prefix', 'last': 'last', 'suffix': 'suffix', 'first': 'first'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nFN:Kenny\r\nN:Cartman;Eric;;;\r\nN:;Kenny;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'a': 'b'}, {'a': 'b', 'names': [{'middle': '', 'prefix': '', 'last': 'Cartman', 'suffix': '', 'first': 'Eric'},
                                            {'middle': '', 'prefix': '', 'last': '', 'suffix': '', 'first': 'Kenny'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'names': ['name']}, {'names': ['name', {'middle': '', 'prefix': '', 'last': 'Cartman', 'suffix': '', 'first': 'Eric'}]}),
))
def testFromVcardNames(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardNames(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'emails': [{'email': 'eric_cartman@yandex.ru'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL;TYPE=WORK:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'emails': [{'email': 'email'}]}, {'emails': [{'email': 'email'}, {'type': ['WORK'], 'email': 'eric_cartman@yandex.ru'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL;TYPE=WORK:eric_cartman@yandex.ru\r\nEMAIL:kenny@yalive.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'emails': [{'type': ['WORK'], 'email': 'eric_cartman@yandex.ru'}, {'email': 'kenny@yalive.ru'}]}),
))
def testFromVcardEmails(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardEmails(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'telephone_numbers': [{'telephone_number': '8800'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL;TYPE=WORK:8800\r\nEND:VCARD\r\n",
        {'telephone_numbers': [{'telephone_number': '1'}]},
        {'telephone_numbers': [{'telephone_number': '1'}, {'type': ['WORK'], 'telephone_number': '8800'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL;TYPE=WORK:8800\r\nTEL:8888\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'telephone_numbers': [{'type': ['WORK'], 'telephone_number': '8800'}, {'telephone_number': '8888'}]}),
))
def testFromVcardPhones(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardPhones(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nNOTE:1\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'notes': ['1'], 'description': '1'}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nNOTE:2\r\nNOTE:3\r\nEND:VCARD\r\n",
        {'notes': ['1']}, {'notes': ['1', '2', '3'], 'description': '1'}),
))
def testFromVcardNotes(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardNotes(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('eventStr', 'expected'), (
    ('--0201', {'year': 0, 'month': 2, 'day': 1}),
    ('--02-01', {'year': 0, 'month': 0, 'day': 0}),
    ('11110000', {'year': 1111, 'month': 0, 'day': 0}),
    ('1111-00-00', {'year': 1111, 'month': 0, 'day': 0}),
    ('--020155555', {'month': 0, 'day': 0, 'year': 0})
))
def testParseDate(eventStr, expected):
    assert_that(parseDate(eventStr), equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nBDAY:--0001\r\nX-ANNIVERSARY:11110001\r\nX-EVOLUTION-ANNIVERSARY:1111-00-01\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'events': [{'year': 0, 'day': 1, 'month': 0, 'type': ['birthday']}, {'year': 1111, 'day': 1, 'month': 0, 'type': ['anniversary']},
                                                        {'year': 1111, 'day': 1, 'month': 0, 'type': ['evolution-anniversary']}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nBDAY:00000201\r\nNOTE:3\r\nEND:VCARD\r\n",
        {'events': [{'event': 'event'}]}, {'events': [{'event': 'event'}, {'month': 2, 'day': 1, 'year': 0, 'type': ['birthday']}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nBDAY:0000-02-01\r\nNOTE:3\r\nEND:VCARD\r\n",
        {'events': [{'event': 'event'}]}, {'events': [{'event': 'event'}, {'month': 2, 'day': 1, 'year': 0, 'type': ['birthday']}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nNOTE:3\r\nEND:VCARD\r\n",
        {'events': [{'event': 'event'}]}, {'events': [{'event': 'event'}]}),
))
def testFromVcardEvents(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardEvents(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nORG;TYPE=type1,type2:company;;;\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'organizations': [{'department': '', 'company': 'company', 'summary': '', 'type': ['type1']}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nORG:company;department\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'organizations': [{'company': 'company', 'department': 'department'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nORG:company;d;s;t\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'organizations': [{'company': 'company', 'department': 'd', 'summary': 's', 'title': 't'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nORG:company;department\r\nTITLE:title\r\nEND:VCARD\r\n",
        {'organizations': [{'company': '1'}]}, {'organizations': [{'company': '1'}, {'company': 'company', 'department': 'department'}]}),
))
def testFromVcardOrganizations(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardOrganizations(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nNICKNAME:1\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'nicknames': ['1']}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nNICKNAME:2\r\nNICKNAME:3\r\nEND:VCARD\r\n",
        {'nicknames': ['1']}, {'nicknames': ['1', '2', '3']}),
))
def testFromVcardNickNames(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardNickNames(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nADR;TYPE=WORK:;;street;city;;;\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'addresses': [{'post_office_box': '',
                                                            'city': 'city',
                                                            'extended': '',
                                                            'country': '',
                                                            'region': '',
                                                            'street': 'street',
                                                            'postal_code': '',
                                                            'type': ['WORK']}]
                            }),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nADR:;;street;city;;;\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'addresses': [{'post_office_box': '',
                                                            'city': 'city',
                                                            'extended': '',
                                                            'country': '',
                                                            'region': '',
                                                            'street': 'street',
                                                            'postal_code': ''}]
                            }),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nADR;TYPE=WORK:;;street1;city1;;;\r\nADR:;;street2;city2;;;\r\nEND:VCARD\r\n",
        {'addresses': [{'adr': 'adr'}]}, {'addresses': [{'adr': 'adr'},
                                                        {'post_office_box': '',
                                                            'city': 'city1',
                                                            'extended': '',
                                                            'country': '',
                                                            'region': '',
                                                            'street': 'street1',
                                                            'postal_code': '',
                                                            'type': ['WORK']},
                                                        {'post_office_box': '',
                                                            'city': 'city2',
                                                            'extended': '',
                                                            'country': '',
                                                            'region': '',
                                                            'street': 'street2',
                                                            'postal_code': ''}]}),
))
def testFromVcardAddresses(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardAddresses(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nURL:1\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'websites': [{'url': '1'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nURL:2\r\nURL:3\r\nEND:VCARD\r\n",
        {'websites': [{'url': '1'}]}, {'websites': [{'url': '1'}, {'url': '2'}, {'url': '3'}]}),
))
def testFromVcardWebSites(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardWebSites(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nUID:1\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'vcard_uids': ['1']}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nUID:2\r\nUID:3\r\nEND:VCARD\r\n",
        {'vcard_uids': ['1']}, {'vcard_uids': ['1', '2', '3']}),
))
def testFromVcardUids(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardUids(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nX-SOCIALPROFILE:profile\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'social_profiles': [{'profile': 'profile'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nX-SOCIALPROFILE;TYPE=WORK:profile1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'social_profiles': [{'profile': 'profile'}]}, {'social_profiles': [{'profile': 'profile'}, {'type': ['WORK'], 'profile': 'profile1'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nX-SOCIALPROFILE;TYPE=WORK:profile1\r\nX-SOCIALPROFILE:profile2\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {}, {'social_profiles': [{'type': ['WORK'], 'profile': 'profile1'}, {'profile': 'profile2'}]}),
))
def testFromVcardSocialProfile(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardSocialProfile(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nIMPP;x-service-type=service_type:service_id\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name'}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nIMPP;x-service-type=service_type:protocol:service_id\r\nEND:VCARD\r\n",
        {'instant_messengers': [{'a': 'b'}]}, {'instant_messengers': [{'a': 'b'}, {'service_type': 'service_type', 'service_id': 'service_id', 'protocol': 'protocol'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nIMPP;x-service-type=service_type::service_id\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'instant_messengers': [{'service_type': 'service_type', 'service_id': 'service_id', 'protocol': ''}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nIMPP;x-service-type=service_type::service_id1\r\nIMPP::service_id2\r\nEND:VCARD\r\n",
        {}, {'instant_messengers': [{'service_type': 'service_type', 'service_id': 'service_id1', 'protocol': ''},
                                    {'service_type': '', 'service_id': 'service_id2', 'protocol': ''}]}),
))
def testFromVcardInstantMessengers(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardInstantMessengers(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nX-TWITTER:twitter\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'social_profiles': [{'profile': 'twitter', 'type': ['twitter']}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nX-TWITTER;TYPE=WORK:twitter\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {'social_profiles': [{'profile': 'profile'}]}, {'social_profiles': [{'profile': 'profile'}, {'type': ['twitter', 'WORK'], 'profile': 'twitter'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nX-TWITTER;TYPE=WORK:twitter\r\nX-JABBER:jabber1\r\nX-JABBER:jabber2\r\nFN:Eric Cartman\r\n"
     "N:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n",
        {}, {'social_profiles': [{'type': ['twitter', 'WORK'], 'profile': 'twitter'}, {'profile': 'jabber1', 'type': ['jabber']}, {'profile': 'jabber2', 'type': ['jabber']}]}),
))
def testFromVcardAdditionalSocialProfile(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardAdditionalSocialProfile(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nX-SKYPE;TYPE=HOME:skype\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'instant_messengers': [{'service_type': 'skype', 'service_id': 'skype', 'type': 'HOME'}]}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nX-SKYPE;TYPE=HOME:skype\r\nX_ICQ;TYPE=work:icq1\r\nX_ICQ:icq2\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'instant_messengers': [{'service_type': 'ICQ', 'service_id': 'icq1', 'type': 'work'}, {'service_type': 'ICQ', 'service_id': 'icq2'},
                                                                    {'service_type': 'skype', 'service_id': 'skype', 'type': 'HOME'}]}),

))
def testFromVcardAdditionalInstantMessengers(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardAdditionalInstantMessengers(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'contact', 'expected'), (
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTZ:Europe/Moscow\r\nEND:VCARD\r\n",
        {'names': 'name'}, {'names': 'name', 'tzs': ['Europe/Moscow']}),
    ("BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTZ:-0500\r\nTZ:America/New_York\r\nEND:VCARD\r\n",
        {'tzs': ['1']}, {'tzs': ['1', '-0500', 'America/New_York']}),
))
def testFromVcardTimeZones(vobjStr, contact, expected):
    vobj = vobject.readOne(vobjStr)
    fromVcardTimeZones(vobj, contact)
    assert_that(contact, equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'expected'), (
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nEND:VCARD\r\n', {}),
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n',
        {'names': [{'middle': '', 'prefix': '', 'last': '', 'suffix': '', 'first': ''}]}),
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nFN:prefix first middle last suffix\r\nN:last;first;middle;prefix;suffix\r\nEND:VCARD\r\n',
        {'names': [{'middle': 'middle', 'prefix': 'prefix', 'last': 'last', 'suffix': 'suffix', 'first': 'first'}]}),
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL;TYPE=HOME:email1\r\nEMAIL;TYPE=WORK:email2\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n',
        {'names': [{'middle': '', 'prefix': '', 'last': '', 'suffix': '', 'first': ''}],
            'emails': [{'type': ['HOME'], 'email': 'email1'}, {'type': ['WORK'], 'email': 'email2'}]}),
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL;TYPE=HOME:email1\r\nEMAIL;TYPE=WORK:email2\r\nFN:\r\nN:;;;;\r\nNICKNAME:1\r\nNICKNAME:2\r\nNICKNAME:3\r\nX-JABBER;TYPE=HOME:home@ya.ru\r\n'
     'X-JABBER;TYPE=WORK:work@ya.ru\r\nX-ANNIVERSARY:11110102\r\nX-EVOLUTION-ANNIVERSARY:1111-03-04\r\nBDAY:--0506\r\nTZ:Europe/Moscow\r\nEND:VCARD\r\n',
        {'names': [{'middle': '', 'prefix': '', 'last': '', 'suffix': '', 'first': ''}], 'tzs': ['Europe/Moscow'],
         'nicknames': ['1', '2', '3'], 'emails': [{'type': ['HOME'], 'email': 'email1'}, {'type': ['WORK'], 'email': 'email2'}],
         'events': [{'month': 5, 'type': ['birthday'], 'day': 6, 'year': 0}, {'month': 1, 'type': ['anniversary'], 'day': 2, 'year': 1111},
                    {'month': 3, 'type': ['evolution-anniversary'], 'day': 4, 'year': 1111}], 'social_profiles': [{'profile': 'home@ya.ru', 'type': ['jabber', 'HOME']},
         {'profile': 'work@ya.ru', 'type': ['jabber', 'WORK']}]}),
))
def testFromVcard(vobjStr, expected):
    vobj = vobject.readOne(vobjStr)
    assert_that(fromVcard(vobj), equal_to(expected))


@pytest.mark.parametrize(('uid', 'vobjStr', 'expected'), (
    ('1', 'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:\r\nN:;;;;\r\nTEL:8800\r\nEND:VCARD\r\n',
        '{"vcard_uids": ["YAAB-1-1"], "names": [{"middle": "", "prefix": "", "last": "", "suffix": "", "first": ""}], '
        '"telephone_numbers": [{"telephone_number": "8800"}], "emails": [{"email": "eric_cartman@yandex.ru"}]}'),
    ('1', 'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n',
        '{"vcard_uids": ["YAAB-1-1"], "names": [{"middle": "", "prefix": "", "last": "Cartman", "suffix": "", "first": "Eric"}], '
        '"telephone_numbers": [{"telephone_number": "8800"}], "emails": [{"email": "eric_cartman@yandex.ru"}]}'),
    ('1', 'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:1\r\nUID:2\r\nUID:3\r\nADR;TYPE=WORK:;;street;city;;;\r\nBDAY:00000001\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:\r\n'
            'IMPP;x-service-type=service_type:protocol:service_id\r\nN:;;;;\r\nNICKNAME:1\r\nNICKNAME:2\r\nNICKNAME:3\r\nNOTE:1\r\nNOTE:2\r\nNOTE:3\r\nORG:company;department\r\n'
            'TEL:8800\r\nTITLE:title\r\nURL:1\r\nX-SOCIALPROFILE;TYPE=WORK:profile\r\nX-ICQ:icq1\r\nX-ICQ:icq2\r\nEND:VCARD\r\n',
        '{'
            '"organizations": [{"department": "department", "company": "company"}], '
            '"addresses": [{"post_office_box": "", "city": "city", "extended": "", "country": "", "region": "", "street": "street", "postal_code": "", "type": ["WORK"]}], '
            '"nicknames": ["1", "2", "3"], '
            '"notes": ["1", "2", "3"], '
            '"websites": [{"url": "1"}], '
            '"emails": [{"email": "eric_cartman@yandex.ru"}], '
            '"instant_messengers": [{"service_type": "service_type", "service_id": "service_id", "protocol": "protocol"}, '
                '{"service_type": "ICQ", "service_id": "icq1"}, {"service_type": "ICQ", "service_id": "icq2"}], '
            '"vcard_uids": ["1", "2", "3"], '
            '"names": [{"middle": "", "prefix": "", "last": "", "suffix": "", "first": ""}], '
            '"telephone_numbers": [{"telephone_number": "8800"}], '
            '"events": [{"month": 0, "type": ["birthday"], "day": 1, "year": 0}], '
            '"social_profiles": [{"profile": "profile", "type": ["WORK"]}], '
            '"description": "1"'
        '}'),
))
def testTransformFromVcard(uid, vobjStr, expected):
    assert_that(transformFromVcard(uid, vobjStr), equal_to(expected))


@pytest.mark.parametrize(('uid', 'contactsStr', 'expected'), (
    ('1', r'{"contacts": []}', r''),
    ('1', r'{"contacts": [{"vcard": {"emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}]}, "contact_id": "1"}]}',
        'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:\r\nN:;;;;\r\nTEL:8800\r\nEND:VCARD\r\n'),
    ('1', r'{"contacts": [{"vcard": {"emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}], "vcard_uids": ["2"]}, "contact_id": "1"}]}',
        'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:2\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:\r\nN:;;;;\r\nTEL:8800\r\nEND:VCARD\r\n'),
    ('2', r'{"contacts": [{"vcard": {"names": [{"first": "Eric", "last": "Cartman"}], "emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}]}, '
        r'"contact_id": "1"}]}',
        'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-2-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric Cartman\r\nN:Cartman;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n'),
    ('1', r'{"contacts": [{"vcard": {"names": [{"first": "Eric"}], "emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}]}, "contact_id": "1"},'
        r'{"vcard": {"names": [{"first": "Kenny"}], "emails": [{"email": "kenny@.alive.ru"}], "vcard_uids": ["14", "08"]}, "contact_id": "2"}]}',
        'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-1-1\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:Eric\r\nN:;Eric;;;\r\nTEL:8800\r\nEND:VCARD\r\n'
        'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:14\r\nUID:08\r\nEMAIL:kenny@.alive.ru\r\nFN:Kenny\r\nN:;Kenny;;;\r\nEND:VCARD\r\n'),
    ('1', r'{"contacts": [{"vcard": {"emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800", "type": ["home"]}], "notes": ["1", "2", "3"], '
            r'"events": [{"type": ["birthday"], "day": 1}], "organizations": [{"company": "company", "department": "department", "title": "title", "type": ["type"]}], '
            r'"nicknames": ["1", "2", "3"], "addresses": [{"street": "street", "city": "city", "type": ["WORK"]}], "websites": [{"url": "1"}], '
            r'"social_profiles": [{"profile": "profile", "type": ["WORK"]}, {"profile": "twitter", "type": ["twitter", "HOME"]}], "instant_messengers": [{"service_id": "service_id", '
            r'"protocol": "protocol", "service_type": "service_type"}], "vcard_uids": ["1", "2", "3"]}, "contact_id": "1"}]}',
        'BEGIN:VCARD\r\nVERSION:3.0\r\nUID:1\r\nUID:2\r\nUID:3\r\nADR;TYPE=WORK:;;street;city;;;\r\nBDAY:00000001\r\nEMAIL:eric_cartman@yandex.ru\r\nFN:\r\n'
        'IMPP;x-service-type=service_type:protocol:service_id\r\nN:;;;;\r\nNICKNAME:1\r\nNICKNAME:2\r\nNICKNAME:3\r\nNOTE:1\r\nNOTE:2\r\nNOTE:3\r\nORG;TYPE=type:company;department;;title\r\n'
        'TEL;TYPE=home:8800\r\nTITLE:title\r\nURL:1\r\nX-SOCIALPROFILE;TYPE=WORK:profile\r\nX-SOCIALPROFILE;TYPE=twitter:twitter\r\nX-TWITTER;TYPE=HOME:twitter\r\nEND:VCARD\r\n'),
))
def testVcardEncoderAndExportContacts(uid, contactsStr, expected):
    assert_that(vcardEncoder(uid, contactsStr), equal_to(expected))
    assert_that(exportContacts(uid, contactsStr), equal_to(expected))


@pytest.mark.parametrize(('vobjStr', 'expected'), (
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nEND:VCARD\r\n', [{'vcard': {}}]),
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n',
        [{'vcard': {'names': [{'middle': '', 'prefix': '', 'last': '', 'suffix': '', 'first': ''}]}}]),
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nFN:prefix first middle last suffix\r\nN:last;first;middle;prefix;suffix\r\nEND:VCARD\r\n',
        [{'vcard': {'names': [{'middle': 'middle', 'prefix': 'prefix', 'last': 'last', 'suffix': 'suffix', 'first': 'first'}]}}]),
    ('BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Name1\r\nN:Name1;;;;\r\nEND:VCARD\r\nBEGIN:VCARD\r\nVERSION:3.0\r\nFN:Name2\r\nN:Name2;;;;\r\nEND:VCARD\r\n',
        [{'vcard': {'names': [{'middle': '', 'prefix': '', 'last': 'Name1', 'suffix': '', 'first': ''}]}},
            {'vcard': {'names': [{'middle': '', 'prefix': '', 'last': 'Name2', 'suffix': '', 'first': ''}]}}]),
))
def testVcardDecoder(vobjStr, expected):
    assert_that(vcardDecoder(vobjStr), equal_to(expected))


@pytest.mark.parametrize(('uid', 'vobjStr', 'expected'), (
    ('1', 'BEGIN:VCARD\r\nVERSION:3.0\r\nEND:VCARD\r\n', r'[{"vcard": {}}]'),
    ('1', 'BEGIN:VCARD\r\nVERSION:3.0\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n',
        r'[{"vcard": {"names": [{"middle": "", "prefix": "", "last": "", "suffix": "", "first": ""}]}}]'),
    ('1', 'BEGIN:VCARD\r\nVERSION:3.0\r\nFN:prefix first middle last suffix\r\nN:last;first;middle;prefix;suffix\r\nEND:VCARD\r\n',
        r'[{"vcard": {"names": [{"middle": "middle", "prefix": "prefix", "last": "last", "suffix": "suffix", "first": "first"}]}}]'),
    ('1', 'BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Name1\r\nN:Name1;;;;\r\nEND:VCARD\r\nBEGIN:VCARD\r\nVERSION:3.0\r\nFN:Name2\r\nN:Name2;;;;\r\nEND:VCARD\r\n',
        r'[{"vcard": {"names": [{"middle": "", "prefix": "", "last": "Name1", "suffix": "", "first": ""}]}}, '
            r'{"vcard": {"names": [{"middle": "", "prefix": "", "last": "Name2", "suffix": "", "first": ""}]}}]'),
))
def testImportContacts(uid, vobjStr, expected):
    assert_that(importContacts(uid, vobjStr), equal_to(expected))
