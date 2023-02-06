# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties

from travel.library.python.dicts.factories.pointsynonym import TPointSynonymFactory
from travel.avia.admin.avia_scripts.sync_with_rasp.sync_www_pointsynonym import sync_pointsynonym
from travel.avia.library.python.common.models.geo import PointSynonym
from travel.avia.library.python.tester.factories import create_pointsynonym

pytestmark = [pytest.mark.dbuser]


def test_sync_www_pointsynonym(rasp_repositories):
    # Setup
    PointSynonym.objects.all().delete()

    create_pointsynonym(
        id=1,
        title='Synonym1',
        language='ru',
    )

    create_pointsynonym(
        id=2,
        title='Synonym2',
        language='tr',
    )

    create_pointsynonym(
        id=3,
        title='Synonym3',
        language='en',
    )

    create_pointsynonym(
        id=4,
        title='Kretingalė',
        language=None,
    )

    rasp_data = [
        # One that matches
        TPointSynonymFactory(
            Id=1,
            Title='Synonym1',
            ContentTypeId=13,
            ObjectId=100500,
            Auto=False,
            Language='ru',
        ),
        # Second one is conflicting by primary key
        TPointSynonymFactory(
            Id=2,
            Title='Synonym2conflict',
            ContentTypeId=13,
            ObjectId=100500,
            Auto=False,
            Language='zz',
        ),
        # Third one with another id.
        TPointSynonymFactory(
            Id=4,
            Title='Synonym3',
            ContentTypeId=13,
            ObjectId=100500,
            Auto=True,
            Language='en',
        ),
        # New synonym
        TPointSynonymFactory(
            Id=5,
            Title='Synonym4',
            ContentTypeId=13,
            ObjectId=100500,
            Auto=False,
            Language='gg',
        ),
        # Collacation conflict
        TPointSynonymFactory(
            Id=6,
            Title='Kretingale',
            ContentTypeId=13,
            ObjectId=100500,
            Auto=False,
            Language='kg',
        )
    ]

    # Test
    # Check two times to
    for i in range(2):
        sync_pointsynonym(rasp_data)

        db_pointsynonym = list(PointSynonym.objects.all())
        assert len(db_pointsynonym) == 6
        assert_that(db_pointsynonym, contains_inanyorder(
            *(has_properties(id=id_) for id_ in [1, 2, 4, 5, 6, 7])
        ))

        assert_that(db_pointsynonym, contains_inanyorder(
            *(
                has_properties(id=id_, language=lang)
                for id_, lang in {1: 'ru', 2: 'zz', 4: 'en', 5: 'gg', 6: 'kg', 7: 'tr'}.items()
            )
        ))
        assert_that(db_pointsynonym, contains_inanyorder(
            has_properties(id=7),
            *(_has_properties_by_rasp_pointsynonym(rd) for rd in rasp_data)
        ))


def _has_properties_by_rasp_pointsynonym(rasp_pointsynonym):
    return has_properties(
        title=rasp_pointsynonym.Title,
        content_type_id=rasp_pointsynonym.ContentTypeId,
        object_id=rasp_pointsynonym.ObjectId,
        search_type=rasp_pointsynonym.SearchType,
    )
