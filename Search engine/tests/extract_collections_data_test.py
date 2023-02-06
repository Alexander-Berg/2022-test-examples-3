#!/usr/bin/env python
# -*- coding: utf-8 -*-

import mock
import pytest

from search.geo.tools.collections.lib.extract_collections_data import (
    extract_author,
    extract_organizations,
    extract_partner_links,
    extract_related_collections,
    extract_rubric,
    _get_booking_link,
    _get_features,
    _get_images,
    _get_links,
    _get_organization,
    _get_tags,
)


LIB_PATH = 'search.geo.tools.collections.lib.extract_collections_data.{}'


class TestExtractRubric:
    def test_extract_rubric__given_rubric_property__returns_it(self):
        properties = [
            {'key': 'item_count', 'value': '5'},
            {'key': 'rubric', 'value': 'Restaurant'},
            {'key': 'partner_name', 'value': 'afisha'},
        ]

        assert extract_rubric(properties) == 'Restaurant'

    def test_extract_rubric__given_no_rubric_property__returns_empty(self):
        properties = [
            {'key': 'item_count', 'value': '5'},
            {'key': 'partner_name', 'value': 'afisha'},
        ]

        assert not extract_rubric(properties)

    def test_extract_rubric__given_empty_properties__returns_empty(self):
        assert not extract_rubric([])

    def test_extract_rubric__given_property_without_key__throws(self):
        with pytest.raises(Exception):
            extract_rubric([{'value': 'Restaurant'}])

    def test_extract_rubric__given_property_without_value__throws(self):
        with pytest.raises(Exception):
            extract_rubric([{'key': 'rubric'}])


class TestExtractRelatedCollections:
    def test_extract_related_collections__given_valid_data__returns_them(self):
        blocks = [{
            'type': 'Links',
            'pages': [
                {'alias': 'gid-po-tolyatti'},
                {'alias': 'gid-magazini-igrushek'}
            ]
        }]

        assert extract_related_collections(blocks) == \
            ['gid-po-tolyatti', 'gid-magazini-igrushek']

    def test_extract_related_collections__given_pages_without_aliases__returns_empty(self):
        blocks = [{
            'type': 'Links',
            'pages': [{'id': 1234}]
        }]

        assert extract_related_collections(blocks) == []

    def test_extract_related_collections__given_empty_pages__returns_empty(self):
        blocks = [{
            'type': 'Links',
            'pages': []
        }]

        assert extract_related_collections(blocks) == []

    def test_extract_related_collections__given_no_pages__throws(self):
        blocks = [{
            'type': 'Links'
        }]

        with pytest.raises(Exception):
            extract_related_collections(blocks)

    def test_extract_related_collections__given_no_links_block__returns_empty(self):
        blocks = [{
            'type': 'Organization'
        }]

        assert extract_related_collections(blocks) == []

    def test_extract_related_collections__given_empty_blocks__returns_empty(self):
        assert extract_related_collections([]) == []

    def test_extract_related_collections__given_block_without_type__throws(self):
        with pytest.raises(Exception):
            extract_related_collections([{}])


class TestExtractPartnerLinks:
    def test_extract_partner_links__given_valid_data__returns_it(self):
        blocks = [{
            'type': 'PartnerLinks',
            'links': 'some-data',
            'titleMask': 'Интересные места и события'
        }]
        partner = {'title': 'KudaGo'}

        result = extract_partner_links(partner, blocks)

        assert 'links' in result
        assert result['title'] == 'Интересные места и события'

    def test_extract_partner_links__given_title_with_mask__replaces_it(self):
        blocks = [{
            'type': 'PartnerLinks',
            'links': 'some-data',
            'titleMask': 'Интересные места и события на %name%'
        }]
        partner = {'title': 'KudaGo'}

        result = extract_partner_links(partner, blocks)

        assert result['title'] == 'Интересные места и события на KudaGo'

    def test_extract_partner_links__given_no_partner_links_block__returns_empty(self):
        blocks = [{'type': 'Organization'}]

        assert extract_partner_links({}, blocks) == {}

    def test_extract_partner_links__given_empty_blocks__returns_empty(self):
        assert extract_partner_links({}, []) == {}

    def test_extract_partner_links__given_block_without_type__throws(self):
        with pytest.raises(Exception):
            extract_partner_links({}, [{}])

    def test_extract_partner_links__given_block_without_links__throws(self):
        blocks = [{
            'type': 'PartnerLinks',
            'titleMask': 'Интересные места и события'
        }]
        partner = {'title': 'KudaGo'}

        with pytest.raises(Exception):
            extract_partner_links(partner, blocks)

    def test_extract_partner_links__given_block_without_titleMask__throws(self):
        blocks = [{
            'type': 'PartnerLinks',
            'links': 'some-data'
        }]
        partner = {'title': 'KudaGo'}

        with pytest.raises(Exception):
            extract_partner_links(partner, blocks)

    def test_extract_partner_links__given_partner_without_title__throws(self):
        blocks = [{
            'type': 'PartnerLinks',
            'links': 'some-data',
            'titleMask': 'Интересные места и события'
        }]

        with pytest.raises(Exception):
            extract_partner_links({}, blocks)


class TestExtractAuthor:
    def test_extract_author__given_valid_data__returns_author(self):
        partner = {
            'title': 'KudaGo',
            'description': 'Лучшее в городе',
            'image': {'urlTemplate': 'https://avatars.mds.yandex.net/...'}
        }

        author = extract_author(partner)
        assert 'name' in author
        assert 'description' in author
        assert 'favicon' in author
        assert 'url' not in author

    def test_extract_author__given_not_empty_url__returns_it(self):
        partner = {
            'title': 'KudaGo',
            'description': 'Лучшее в городе',
            'image': {'urlTemplate': 'https://avatars.mds.yandex.net/...'},
            'url': 'https://kudago.com/'
        }

        assert extract_author(partner)['url'] == 'https://kudago.com/'

    def test_extract_author__given_empty_url__wont_return_it(self):
        partner = {
            'title': 'KudaGo',
            'description': 'Лучшее в городе',
            'image': {'urlTemplate': 'https://avatars.mds.yandex.net/...'},
            'url': ''
        }

        assert 'url' not in extract_author(partner)

    def test_extract_author__given_partner_without_title__throws(self):
        partner = {
            'description': 'Лучшее в городе',
            'image': {'urlTemplate': 'https://avatars.mds.yandex.net/...'},
        }

        with pytest.raises(Exception):
            extract_author(partner)

    def test_extract_author__given_partner_without_description__throws(self):
        partner = {
            'title': 'KudaGo',
            'image': {'urlTemplate': 'https://avatars.mds.yandex.net/...'},
        }

        with pytest.raises(Exception):
            extract_author(partner)

    def test_extract_author__given_partner_without_image__throws(self):
        partner = {
            'title': 'KudaGo',
            'description': 'Лучшее в городе',
        }

        with pytest.raises(Exception):
            extract_author(partner)


class TestExtractOrganizations:
    @mock.patch(LIB_PATH.format('_get_organization'))
    def test_extract_organizations__given_open_orgs__returns_them(self, _get_organization):
        _get_organization.side_effect = lambda _, org: org['oid']

        blocks = [
            {'oid': '1', 'type': 'Organization'},
            {'oid': '2', 'type': 'Organization'}
        ]

        assert extract_organizations({}, blocks) == ['1', '2']

    @mock.patch(LIB_PATH.format('_get_organization'))
    def test_extract_organizations__given_duplicate_oids__returns_without_duplicates(self, _get_organization):
        _get_organization.side_effect = lambda _, org: org['oid']
        blocks = [
            {'oid': '1', 'type': 'Organization'},
            {'oid': '1', 'type': 'Organization'}
        ]

        assert len(extract_organizations({}, blocks)) == 1

    def test_extract_organizations__given_no_org_block__returns_empty(self):
        blocks = [{'type': 'Links'}]

        assert extract_organizations({}, blocks) == []

    def test_extract_organizations__given_closed_org__returns_empty(self):
        blocks = [{
            'type': 'Organization',
            'closed': 'permanent'
        }]

        assert extract_organizations({}, blocks) == []

    def test_extract_organizations__given_removed_org__returns_empty(self):
        blocks = [{
            'type': 'Organization',
            'removed': True
        }]

        assert extract_organizations({}, blocks) == []

    def test_extract_organizations__given_empty_blocks__returns_empty(self):
        assert extract_organizations({}, []) == []

    def test_extract_organizations__given_block_without_type__throws(self):
        with pytest.raises(Exception):
            extract_organizations({}, [{}])


class TestGetImages:
    def test_get_images__given_valid_data__returns_url_templates(self):
        org = {
            'images': [
                {'urlTemplate': 'url1', 'caption': 'title'},
                {'urlTemplate': 'url2', 'caption': 'title'}
            ]
        }

        assert _get_images(org) == [{'urlTemplate': 'url1'}, {'urlTemplate': 'url2'}]

    def test_get_images__given_images_without_urlTemplate__throws(self):
        with pytest.raises(Exception):
            _get_images({'images': [{}]})

    def test_get_images__given_org_without_images__returns_empty(self):
        assert _get_images({}) == []

    def test_get_images__given_empty_images__returns_empty(self):
        assert _get_images({'images': []}) == []


class TestGetTags:
    def test_get_tags__given_style__returns_it(self):
        assert _get_tags({'style': 'poor'}) == ['poor']

    def test_get_tags__given_no_style__throws(self):
        with pytest.raises(Exception):
            _get_tags({})

    def test_get_tags__given_placemarkIcon__returns_it(self):
        org = {
            'style': 'poor',
            'placemarkIcon': {'tag': 'cinemas'}
        }

        assert 'placemarkIcon:cinemas' in _get_tags(org)

    def test_get_tags__given_placemarkIcon_without_tag__throws(self):
        org = {
            'style': 'poor',
            'placemarkIcon': {}
        }

        with pytest.raises(Exception):
            _get_tags(org)

    def test_get_tags__given_paragraphIcon__returns_it(self):
        org = {
            'style': 'poor',
            'paragraphIcon': {'tag': 'cinemas'}
        }

        assert 'paragraphIcon:cinemas' in _get_tags(org)

    def test_get_tags__given_paragraphIcon_without_tag__throws(self):
        org = {
            'style': 'poor',
            'paragraphIcon': {}
        }

        with pytest.raises(Exception):
            _get_tags(org)


class TestGetFeatures:
    def test_get_features__given_valid_data__returns_it(self):
        org = {
            'features': [{
                'key': 'average_bill2',
                'name': 'Средняя цена',
                'value': '1000',
                'useRemoteValue': True
            }]
        }

        result = _get_features(org)
        assert result == [{'key': 'average_bill2',
                           'name': 'Средняя цена',
                           'value': '1000'}]

    def test_get_features__given_empty_features__returns_empty(self):
        assert _get_features({'features': []}) == []

    def test_get_features__given_org_without_features__returns_empty(self):
        assert _get_features({}) == []

    def test_get_features__given_feature_without_key__throws(self):
        org = {
            'features': [{
                'name': 'Средняя цена',
                'value': '1000',
            }]
        }
        with pytest.raises(Exception):
            _get_features(org)

    def test_get_features__given_feature_without_name__throws(self):
        org = {
            'features': [{
                'key': 'average_bill2',
                'value': '1000',
            }]
        }
        with pytest.raises(Exception):
            _get_features(org)

    def test_get_features__given_feature_without_value__throws(self):
        org = {
            'features': [{
                'key': 'average_bill2',
                'name': 'Средняя цена',
            }]
        }
        with pytest.raises(Exception):
            _get_features(org)


class TestGetBookingLink:
    def get_partner(self):
        return {
            'aref': '#afisha',
            'linkType': 'showtimes',
            'linkTitle': ''
        }

    def get_org(self):
        partner = self.get_partner()
        return {
            'bookingLink': {
                'show': True,
                'useDefaultTitle': True,
                'title': ''
            },
            'bookingLinksInfo': [{
                'aref': partner['aref'],
                'type': partner['linkType'],
                'href': 'afisha.yandex.ru'
            }]
        }

    def test_get_booking_link__given_no_partner_aref__returns_none(self):
        partner = self.get_partner()
        del partner['aref']

        assert not _get_booking_link(partner, {})

    def test_get_booking_link__given_no_bookingLink__returns_none(self):
        org = self.get_org()
        del org['bookingLink']

        assert not _get_booking_link(self.get_partner(), org)

    def test_get_booking_link__when_bookingLink_show_is_false__returns_none(self):
        org = self.get_org()
        org['bookingLink']['show'] = False

        assert not _get_booking_link(self.get_partner(), org)

    def test_get_booking_link__given_bookingLink_without_show__throws(self):
        org = self.get_org()
        del org['bookingLink']['show']

        with pytest.raises(Exception):
            _get_booking_link(self.get_partner(), org)

    def test_get_booking_link__given_no_bookingLinksInfo__returns_none(self):
        org = self.get_org()
        del org['bookingLinksInfo']

        assert not _get_booking_link(self.get_partner(), org)

    def test_get_booking_link__given_empty_bookingLinksInfo__returns_none(self):
        org = self.get_org()
        org['bookingLinksInfo'] = []

        assert not _get_booking_link(self.get_partner(), org)

    def test_get_booking_link__given_valid_data__returns_link(self):
        result = _get_booking_link(self.get_partner(), self.get_org())

        assert result['tags'] == ['showtimes']
        assert result['url'] == 'afisha.yandex.ru'

    def test_get_booking_link__when_useDefaultTitle_is_enabled__returns_partner_title(self):
        partner = self.get_partner()
        partner['linkTitle'] = 'Посмотреть афишу'

        org = self.get_org()
        org['bookingLink']['useDefaultTitle'] = True

        result = _get_booking_link(partner, org)
        assert result['title'] == partner['linkTitle']

    def test_get_booking_link__when_useDefaultTitle_is_disabled__returns_booking_link_title(self):
        org = self.get_org()
        org['bookingLink']['useDefaultTitle'] = False
        org['bookingLink']['title'] = 'Забронировать билеты'

        result = _get_booking_link(self.get_partner(), org)
        assert result['title'] == org['bookingLink']['title']

    def test_get_booking_link__when_partner_aref_doesnt_match_link_aref__returns_none(self):
        partner = self.get_partner()
        partner['aref'] = '#eda'

        org = self.get_org()
        org['bookingLinksInfo'][0]['aref'] = '#afisha'

        assert not _get_booking_link(partner, org)

    def test_get_booking_link__when_link_has_no_aref__returns_none(self):
        org = self.get_org()
        del org['bookingLinksInfo'][0]['aref']

        assert not _get_booking_link(self.get_partner(), org)

    def test_get_booking_link__when_partner_linkType_doesnt_match_link_type__returns_none(self):
        partner = self.get_partner()
        partner['linkType'] = 'booking'

        org = self.get_org()
        org['bookingLinksInfo'][0]['type'] = 'showtimes'

        assert not _get_booking_link(partner, org)

    def test_get_booking_link__given_no_partner_linkType__returns_link_with_booking_type(self):
        partner = self.get_partner()
        del partner['linkType']

        org = self.get_org()
        org['bookingLinksInfo'][0]['type'] = 'booking'

        assert _get_booking_link(partner, org)['tags'] == ['booking']

    def test_get_booking_link__when_link_has_no_type__returns_none(self):
        org = self.get_org()
        del org['bookingLinksInfo'][0]['type']

        assert not _get_booking_link(self.get_partner(), org)


class TestGetLinks:
    def test_get_links__given_links__returns_it(self):
        org = {
            'links': [{
                'name': 'Хочу в музей!',
                'url': 'https://pushkinmuseum.art'
            }]
        }

        assert _get_links({}, org) == [{'title': 'Хочу в музей!',
                                        'url': 'https://pushkinmuseum.art'}]

    def test_get_links__given_links_without_name__throws(self):
        org = {
            'links': [{
                'url': 'https://pushkinmuseum.art'
            }]
        }

        with pytest.raises(Exception):
            _get_links({}, org)

    def test_get_links__given_links_without_url__throws(self):
        org = {
            'links': [{
                'name': 'Хочу в музей!',
            }]
        }

        with pytest.raises(Exception):
            _get_links({}, org)

    def test_get_links__given_org_without_links__returns_empty(self):
        assert _get_links({}, {}) == []

    @mock.patch(LIB_PATH.format('_get_booking_link'))
    def test_get_links__given_booking_link__returns_it(self, _get_booking_link):
        booking_link = {
            'title': 'Посмотреть афишу',
            'url': 'https://afisha.yandex.ru',
            'tags': ['showtimes']
        }
        _get_booking_link.return_value = booking_link

        result = _get_links({}, {})

        assert booking_link in result


class TestGetOrganization:
    def get_org(self):
        return {
            'oid': '1',
            'title': {
                'value': 'Эрмитаж'
            },
            'coordinate': {
                'lat': '33.5',
                'lon': '55.6'
            },
            'style': 'poor'
        }

    def test_get_organization__given_valid_data__returns_it(self):
        org = self.get_org()
        result = _get_organization({}, org)

        assert result['oid'] == '1'
        assert 'title' in result
        assert 'coordinate' in result
        assert 'tags' in result

    def test_get_organization__given_no_oid__throws(self):
        org = self.get_org()
        del org['oid']

        with pytest.raises(Exception):
            _get_organization({}, org)

    def test_get_organization__given_no_coordinate__throws(self):
        org = self.get_org()
        del org['coordinate']

        with pytest.raises(Exception):
            _get_organization({}, org)

    def test_get_organization__given_customTitle__returns_it(self):
        org = self.get_org()
        org.update({'customTitle': 'Государственный Эрмитаж'})

        assert _get_organization({}, org)['title'] == 'Государственный Эрмитаж'

    def test_get_organization__given_empty_customTitle__returns_title(self):
        org = self.get_org()
        org.update({'customTitle': ''})
        org.update({'title': {'value': 'Эрмитаж'}})

        assert _get_organization({}, org)['title'] == 'Эрмитаж'

    def test_get_organization__given_no_title_value__throws(self):
        org = self.get_org()
        del org['title']['value']

        with pytest.raises(Exception):
            _get_organization({}, org)

    def test_get_organization__given_no_title__throws(self):
        org = self.get_org()
        del org['title']

        with pytest.raises(Exception):
            _get_organization({}, org)

    @mock.patch(LIB_PATH.format('_get_images'))
    def test_get_organization__always__calls_get_images(self, _get_images):
        org = self.get_org()

        _get_organization({}, org)

        _get_images.assert_called_once_with(org)

    @mock.patch(LIB_PATH.format('_get_features'))
    def test_get_organization__always__calls_get_features(self, _get_features):
        org = self.get_org()

        _get_organization({}, org)

        _get_features.assert_called_once_with(org)

    def test_get_organization__given_description__returns_it(self):
        org = self.get_org()
        org.update({'description': 'Лучший музей'})

        assert _get_organization({}, org)['description'] == 'Лучший музей'

    def test_get_organization__given_empty_description__wont_return_it(self):
        org = self.get_org()
        org.update({'description': ''})

        assert 'description' not in _get_organization({}, org)

    def test_get_organization__given_sentence__returns_annotation(self):
        org = self.get_org()
        org.update({'sentence': 'Лучший музей'})

        assert _get_organization({}, org)['annotation'] == 'Лучший музей'

    def test_get_organization__given_empty_sentence__wont_return_annotation(self):
        org = self.get_org()
        org.update({'sentence': ''})

        assert 'annotation' not in _get_organization({}, org)

    @mock.patch(LIB_PATH.format('_get_links'))
    def test_get_organization__given_links__calls_get_links(self, _get_links):
        org = self.get_org()
        org.update({'links': []})

        _get_organization({}, org)

        _get_links.assert_called_once_with({}, org)

    @mock.patch(LIB_PATH.format('_get_links'))
    def test_get_organization__given_bookingLink__calls_get_links(self, _get_links):
        org = self.get_org()
        org.update({'bookingLink': {}})

        _get_organization({}, org)

        _get_links.assert_called_once_with({}, org)
