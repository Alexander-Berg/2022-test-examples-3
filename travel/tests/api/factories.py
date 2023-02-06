# -*- coding: utf-8 -*-
from travel.avia.library.python.tester.factories import ProcessStringKeyMixin, ModelFactory

from travel.avia.library.python.common.models.partner import RegionalizePartnerQueryRule

from travel.avia.ticket_daemon.ticket_daemon.models import QueryBlackList


class RegionalizePartnerQueryRuleFactory(ProcessStringKeyMixin, ModelFactory):
    Model = RegionalizePartnerQueryRule


create_regionalizepartnerqueryrule = RegionalizePartnerQueryRuleFactory()


class QueryBlackListFactory(ProcessStringKeyMixin, ModelFactory):
    Model = QueryBlackList


create_queryblacklist = QueryBlackListFactory()
