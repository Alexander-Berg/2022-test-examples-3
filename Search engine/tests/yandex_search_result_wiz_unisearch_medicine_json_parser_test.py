from test_utils import TestParser


class TestYandexSearchResultWizUnisearchMedicineJSONParser(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")
        url_0 = "https://prodoctorov.ru/pushkino/vrach/173716-devyataev/"
        url_2 = "https://zoon.ru/msk/p-doctor/svetlana_leonovna_zhaboeva/"
        clinics_0_0 = "55.971878,37.910816"
        address_0_0 = "Ивантеевка, пр-д Детский, д. 8"
        name_0_0 = "Медицинский центр «Мой Доктор» на Детском проезде"

        assert url_0 == example["components"][0]["componentUrl"]["pageUrl"]
        assert url_2 == example["components"][2]["componentUrl"]["pageUrl"]
        assert clinics_0_0 == example["components"][0]["json.clinics"][0]["position"]
        assert address_0_0 == example["components"][0]["json.clinics"][0]["address"]
        assert name_0_0 == example["components"][0]["json.clinics"][0]["name"]
