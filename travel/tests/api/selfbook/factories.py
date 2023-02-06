# -*- coding: utf-8 -*-
from travel.avia.library.python.tester.factories import ProcessStringKeyMixin, ModelFactory

from travel.avia.library.python.avia_data.models.selfbook import (
    SelfBookCompany, SelfBookDirection, SelfBookPartner,
    SelfBookNationalVersion, SelfBookRule,
)


class SelfBookCompanyFactory(ProcessStringKeyMixin, ModelFactory):
    Model = SelfBookCompany


create_selfbookcompany = SelfBookCompanyFactory()


class SelfBookDirectionFactory(ProcessStringKeyMixin, ModelFactory):
    Model = SelfBookDirection


create_selfbookdirection = SelfBookDirectionFactory()


class SelfBookNationalVersionFactory(ProcessStringKeyMixin, ModelFactory):
    Model = SelfBookNationalVersion


create_selfbooknationalversion = SelfBookNationalVersionFactory()


class SelfBookPartnerFactory(ProcessStringKeyMixin, ModelFactory):
    Model = SelfBookPartner


create_selfbookpartner = SelfBookPartnerFactory()


class SelfBookRuleFactory(ProcessStringKeyMixin, ModelFactory):
    Model = SelfBookRule


create_selfbookrule = SelfBookRuleFactory()
