import unittest

from travel.avia.aeroflot_queue_proxy.lib.remove_personal_data import remove_personal_data


class ConsumerTests(unittest.TestCase):
    def test_remove_personal_data(self):
        initial_message = '''
        <?xml version="1.0" encoding="UTF-8"?>
        <cepMessage>
            <IATA_Notification_Email>ivan_ivanov@example.com</IATA_Notification_Email>
            <IATA_Notification_Phone>+77777777777</IATA_Notification_Phone>
            <firstName>IVAN</firstName>
            <fqtNumber>228</fqtNumber>
            <lastName>EXAMPLE</lastName>
            <IATA_Notification_Email/>
        </cepMessage>
        '''
        expected_message = '''
        <?xml version="1.0" encoding="UTF-8"?>
        <cepMessage>
            <IATA_Notification_Email>******</IATA_Notification_Email>
            <IATA_Notification_Phone>******</IATA_Notification_Phone>
            <firstName>******</firstName>
            <fqtNumber>******</fqtNumber>
            <lastName>******</lastName>
            <IATA_Notification_Email/>
        </cepMessage>
        '''

        actual_message = remove_personal_data(initial_message)

        self.assertEqual(actual_message, expected_message)

    def test_remove_personal_data_inline(self):
        """
        test against greedy regexes
        """

        initial_message = '\t<?xml version="1.0" encoding="UTF-8"?>\n\t\t<cepMessage>\n\t\t\t<IATA_Notification_Email' \
                          '>ivan_ivanov@example.com</IATA_Notification_Email>\n\t\t\t<IATA_Notification_Phone' \
                          '>+77777777777</IATA_Notification_Phone>\n\t\t\t<firstName>IVAN</firstName>\n\t\t\t' \
                          '<fqtNumber>228</fqtNumber>\n\t\t\t<lastName>EXAMPLE</lastName>\n\t\t\t' \
                          '<IATA_Notification_Email/>\n\t\t</cepMessage> '
        expected_message = '\t<?xml version="1.0" encoding="UTF-8"?>\n\t\t<cepMessage>\n\t\t\t' \
                           '<IATA_Notification_Email>******</IATA_Notification_Email>\n\t\t\t<IATA_Notification_Phone' \
                           '>******</IATA_Notification_Phone>\n\t\t\t<firstName>******</firstName>\n\t\t\t<fqtNumber' \
                           '>******</fqtNumber>\n\t\t\t<lastName>******</lastName>\n\t\t\t<IATA_Notification_Email' \
                           '/>\n\t\t</cepMessage> '

        actual_message = remove_personal_data(initial_message)

        self.assertEqual(actual_message, expected_message)


if __name__ == '__main__':
    unittest.main()
