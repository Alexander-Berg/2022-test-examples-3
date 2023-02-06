# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

from django.utils.encoding import force_text

from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import TicketPaymentFactory, TrainOrderFactory, PassengerFactory
from travel.rasp.train_api.train_purchase.core.models import Ticket


def create_order(number_of_passengers=2, **kwargs):
    """
    Чтобы это работало нужно помечать тесты с помощью pytest.mark.dbuser и pytest.mark.mongouser
    """
    if 'passengers' not in kwargs:
        kwargs['passengers'] = [
            PassengerFactory(tickets=[Ticket(
                blank_id=force_text(i + 1),
                payment=TicketPaymentFactory(
                    amount=Decimal(10),
                    partner_fee=Decimal(30),
                    partner_refund_fee=Decimal(20),
                    fee=Decimal(70),
                ),
                places=[force_text(i + 1)],
                rzhd_status=RzhdStatus.REMOTE_CHECK_IN
            )]) for i in range(number_of_passengers)
        ]
    return TrainOrderFactory(
        partner=kwargs.pop('partner', TrainPartner.UFS),
        train_number='001A',
        train_ticket_number='002A',
        passengers=kwargs.pop('passengers'),
        **kwargs
    )
