import base64
import factory
import random

from cars.fines.factories.fine_feed import FineFeedGibddFactory


class AutoCodeFinesClientStub:

    def __init__(self):
        self.fines_feed = []
        self._confirmations_feed = []
        self.load_stub_fines()
        self.load_stub_payment_confirmations()

    def get_new_fines(self, offset=0, limit=10):
        return self.fines_feed[offset:offset+limit]

    def get_new_paid_fines(self, offset=0, limit=10):
        return self.confirmations_feed[offset:offset+limit]

    def load_stub_payment_confirmations(self):
        if self.fines_feed:
            self.confirmations_feed = [random.choice(self.fines_feed)]

    def load_stub_fines(self, feed_params_list=None, count=1):
        if not feed_params_list:
            feed_params_list = [{} for _ in range(count)]

        self.fines_feed = [
            factory.build(dict, FACTORY_CLASS=FineFeedGibddFactory, **feed_params)
            for feed_params in feed_params_list
        ]

    def load_stub_fines_realdata(self):
        self.fines_feed = [
            {
                'violationDocumentType': 'Sts',
                'rulingNumber': '18810177171224235219',
                'violationPlace': 'ВАРШАВСКОЕ Ш., д.85, корп.1, НАГОРНЫЙ (ЮАО) Р-Н, МОСКВА Г.',
                'sumToPay': 500.0,
                'discountDate': '15.01.2018',
                'rulingDate': '24.12.2017',
                'odpsName': 'ЦАФАП ОДД ГИБДД ГУ МВД России по г.Москве',
                'violationDocumentNumber': '7744276262',
                'odpsCode': '45519',
                'hasPhoto': True,
                'id': '308145',
                'violationDateWithTime': '24.12.2017 18:02:14',
                'articleKoap': '12.16.1 - Несоблюдение требований знаков или разметки, за искл.случаев, предусм.др.статьями гл.12'
            },
            {
                'violationDocumentType': 'Sts',
                'rulingNumber': '18810177180102231144',
                'violationPlace': 'ЛЕНИНГРАДСКОЕ ШОССЕ, 29КМ 350М (ИЗ ЦЕНТРА), МОСКВА Г.',
                'sumToPay': 500.0,
                'discountDate': '22.01.2018',
                'rulingDate': '02.01.2018',
                'odpsName': 'ЦАФАП ОДД ГИБДД ГУ МВД России по г.Москве',
                'violationDocumentNumber': '7756442440',
                'odpsCode': '45519',
                'hasPhoto': True,
                'id': '354210',
                'violationDateWithTime': '02.01.2018 04:15:11',
                'articleKoap': '12.09.2 - Превышение скорости движения ТС от 20 до 40 км/ч'
            },
            {
                'violationDocumentType': 'Sts',
                'rulingNumber': '18810177180103366612',
                'violationPlace': 'ТТК, ВНУТРЕННЕЕ КОЛЬЦО, СПАРТАКОВСКАЯ ПЛ., Д.16/15, СТР.17, МОСКВА Г.',
                'sumToPay': 500.0,
                'discountDate': '23.01.2018',
                'rulingDate': '03.01.2018',
                'odpsName': 'ЦАФАП ОДД ГИБДД ГУ МВД России по г.Москве',
                'violationDocumentNumber': '7756442440',
                'odpsCode': '45519',
                'hasPhoto': True,
                'id': '362616',
                'violationDateWithTime': '03.01.2018 16:44:09',
                'articleKoap': '12.09.2 - Превышение скорости движения ТС от 20 до 40 км/ч'
            },
        ]

    def load_photo_feed(self):
        self.fines_photos_feed = [
            base64.b64encode(b'<photo0_content>').decode('ascii'),
            base64.b64encode(b'<photo1_content>').decode('ascii'),
        ]

    def load_no_photo_feed(self):
        self.fines_photos_feed = []

    def get_violation_photos(self, ruling_number):
        return self.fines_photos_feed[:]
