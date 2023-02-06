from fan.testutils import TestCase


class UnsubscribeListTest(TestCase):
    def test_upsert_clean(self):
        email = " TeSt+extra_data@NaroD.rU   "
        result_email = "test+extra_data@yandex.ru"

        self.unsubscribe_list.upsert_element(email=email)
        element = self.unsubscribe_list.elements.first()

        self.assertTrue(element)
        self.assertEqual(element.email, result_email)
