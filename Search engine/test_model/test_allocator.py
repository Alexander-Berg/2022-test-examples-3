# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import typing  # noqa

from search.martylib.db_utils import session_scope
from search.priemka.yappy.proto.structures.api_pb2 import ApiBetaComponent, ApiBeta
from search.priemka.yappy.proto.structures.slot_pb2 import Slot
from search.priemka.yappy.sqla.yappy import model
from search.priemka.yappy.src.model.model_service.workers.allocator import Allocator
from search.priemka.yappy.src.yappy_lib.utils import enum_to_string
from sqlalchemy.orm import Session

from search.priemka.yappy.tests.utils.test_cases import LineageIITestCase


class TestAllocator(LineageIITestCase):
    SLOTS_COUNT = 10

    @classmethod
    def setUpClass(cls):
        super(TestAllocator, cls).setUpClass()

    def create_test_data(self):
        default_component_type = model.ComponentType(name='default_component_type')
        default_quota = model.Quota(name='default_quota')

        with session_scope() as session:
            session.add(default_component_type)
            session.add(default_quota)
            for i in range(self.SLOTS_COUNT):
                session.add(
                    model.Slot(
                        id='default_slot_{}'.format(i),
                        type=enum_to_string(Slot.Type, Slot.Type.NANNY),
                        quota_name=default_quota.name
                    )
                )

    @staticmethod
    def get_empty_slots_count(session):
        # type: (Session) -> int
        return session.query(model.Slot).filter(model.Slot.yappy__BetaComponent_id.is_(None)).count()

    @staticmethod
    def get_not_empty_slots(session):
        # type: (Session) -> typing.List[model.Slot]
        return session.query(model.Slot).filter(model.Slot.yappy__BetaComponent_id.isnot(None)).all()

    @LineageIITestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def create_test_beta(self, name, components_count=1):
        # type: (str, int, int) -> None
        components = [
            ApiBetaComponent(
                type='default_component_type',
                quota='default_quota'
            )
            for _ in range(components_count)
        ]

        request = ApiBeta(
            name=name,
            components=components,
        )

        with self.mock_request() as ctx:
            self.lineage2.create_beta(request, ctx)

    @LineageIITestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_allocate_deallocate_beta(self):
        # create beta
        self.create_test_beta('test-beta-1', 2)

        with session_scope() as session:

            # allocating beta
            beta = session.query(model.Beta).filter(model.Beta.name == 'test-beta-1').first()
            self.assertFalse(beta.allocated)
            Allocator.allocate_beta(beta.name, session)
            session.commit()

            # test that beta was allocated
            self.assertTrue(beta.allocated)
            self.assertEqual(self.get_empty_slots_count(session), self.SLOTS_COUNT - 2)

            # deallocating beta
            Allocator.deallocate_beta(beta.name, session)
            session.commit()

            # test that beta was deallocated
            self.assertFalse(beta.allocated)
            self.assertEqual(self.get_empty_slots_count(session), self.SLOTS_COUNT)

    @LineageIITestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_reallocate_beta(self):
        self.create_test_beta('test-beta-2', 3)

        with session_scope() as session:  # type: Session

            # allocating beta
            beta = session.query(model.Beta).filter(model.Beta.name == 'test-beta-2').first()
            Allocator.allocate_beta(beta.name, session)
            session.commit()

            # test to reallocate fully allocated beta
            Allocator.reallocate_beta(beta.name, session)
            session.commit()
            self.assertTrue(beta.allocated)

            # deleting 2 slots
            slots = self.get_not_empty_slots(session)
            self.logger.info('slots in use %s', str(slots))
            self.assertEqual(len(slots), 3)
            self.logger.info('deleting 2 slots')
            session.delete(slots[0])
            session.delete(slots[1])
            session.commit()

            # reallocating beta
            Allocator.reallocate_beta(beta.name, session)
            session.commit()

            # test that beta was reallocated successfully
            slots = self.get_not_empty_slots(session)
            self.logger.info('slots in use after deleting %s', str(slots))
            self.assertEqual(self.get_empty_slots_count(session), self.SLOTS_COUNT - 5)
            self.assertEqual(len(slots), 3)

            # deleting all empty slots and one slot from beta (after next reallocation beta must be deallocated)
            deleted_slots_count = self.get_empty_slots_count(session)
            session.query(model.Slot).filter(model.Slot.yappy__BetaComponent_id.is_(None)).delete()
            session.query(model.Slot).filter(model.Slot.id == slots[0].id).delete()
            session.commit()

            # reallocating beta
            self.assertEqual(self.get_empty_slots_count(session), 0)
            Allocator.reallocate_beta(beta.name, session)
            session.commit()

            # test that beta was reallocated failure
            self.assertFalse(beta.allocated)
            self.assertEqual(self.get_empty_slots_count(session), self.SLOTS_COUNT - deleted_slots_count - 3)
