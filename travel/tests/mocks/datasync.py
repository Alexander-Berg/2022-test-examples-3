# -*- encoding: utf-8 -*-
from travel.avia.travelers.tests.factory import Factory


class MockDataSyncClient(object):
    def __init__(self):
        self._traveler = None
        self._passengers = None
        self._documents = None
        self._bonus_cards = None
        self.saved = {}

    def set_traveler(self, traveler):
        self._traveler = traveler

    def get_traveler(self, *args):
        if self._traveler:
            return self._traveler
        return Factory.traveler()

    def get_passenger(self, *args):
        if self._passengers:
            return self._passengers[0]
        return Factory.passenger()

    def get_passengers(self, *args):
        if self._passengers:
            return self._passengers
        return [Factory.passenger()]

    def save_passenger(self, *args):
        _, _, self.saved['passenger'] = args
        return self.get_passenger()

    def get_passenger_documents(self, *args):
        if self._documents:
            return self._documents
        return [Factory.document()]

    def set_documents(self, documents):
        self._documents = documents

    def get_document(self, *args):
        if self._documents:
            return self._documents[0]
        return Factory.document()

    def get_documents(self, *args):
        if self._documents:
            return self._documents
        return [Factory.document()]

    def save_document(self, *args):
        _, _, _, self.saved['document'] = args
        return self.get_document()

    def get_passenger_bonus_cards(self, *args):
        if self._bonus_cards:
            return self._bonus_cards
        return [Factory.bonus_card()]

    def get_bonus_card(self, *args):
        if self._bonus_cards:
            return self._bonus_cards[0]
        return Factory.bonus_card()

    def get_bonus_cards(self, *args):
        if self._bonus_cards:
            return self._bonus_cards
        return [Factory.bonus_card()]

    def save_bonus_card(self, *args):
        _, _, _, self.saved['bonus_card'] = args
        return self.get_bonus_card()
