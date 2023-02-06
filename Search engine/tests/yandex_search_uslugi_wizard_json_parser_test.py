from test_utils import TestParser


class TestYandexSearchUslugiWizardJSONParser(TestParser):

    def test_base(self):
        example = self.parse_file("test.json")
        components = example["components"]

        assert components[0]["componentUrl"]["pageUrl"] == "https://profi.ru/remont/santehnika/"
        assert components[1]["componentUrl"]["pageUrl"] == "https://www.Avito.ru/moskva/predlozheniya_uslug/remont_stroitelstvo/santekhnika-ASgBAgICAkSYC8CfAcQVsvUB"
        assert components[2]["componentUrl"]["pageUrl"] == "https://uslugio.com/moskva/1/11/santehnik"
        assert components[3]["componentUrl"]["pageUrl"] == "http://yandex.ru/uslugi/people_services"

        assert example["tags.PassedTheClassifier"] is True
        assert example["long.PredictedIntentPosition"] == 4
        assert example["tags.NoSaasAnswer"] is False
        assert example["tags.BlendedWizard"] is True
        assert example["long.NumberOfOffers"] == 92

        assert components[0]["json.WizardInfo"] == {}
        assert components[0]["tags.IsYdoWizard"] is False
        assert components[0]["long.PromotedWorkers"] == 0
        assert components[1]["json.WizardInfo"] == {}
        assert components[1]["tags.IsYdoWizard"] is False
        assert components[2]["json.WizardInfo"] == {}
        assert components[2]["tags.IsYdoWizard"] is False
        assert components[3]["json.WizardInfo"]["orgs_count"] == 476
        assert components[3]["json.WizardInfo"]["persons_count"] == 1324
        assert components[3]["json.WizardInfo"]["workers_shown"] == 5
        assert components[3]["json.WizardInfo"]["workers"][0]["rating"] == 4.879310131
        assert components[3]["json.WizardInfo"]["workers"][1]["reviews_count"] == 5
        assert components[3]["json.WizardInfo"]["workers"][2]["is_org"] is False
        assert components[3]["tags.IsYdoWizard"] is True
