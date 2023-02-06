from django.test import TestCase

from fan.utils.xls import XLSFile


def _get_data_file(filename):
    import os.path

    filename = os.path.join(os.path.abspath(os.path.dirname(__file__)), "data", filename)
    return open(filename, "rb")


class XMLTestCase(TestCase):
    def test_xls_load(self):

        xls1 = XLSFile(data=_get_data_file("xls/simple-email-list.xlsx"))
        data = xls1.sheet_as_csv(sheet_name=0)
        self.assertEqual(data, """a@b.c\r\nd@e.f\r\n""")

        xls2 = XLSFile(data=_get_data_file("xls/list-with-columns.xlsx").read())
        data = xls2.sheet_as_csv(sheet_name=0)
        self.assertEqual(data, """email,name,value\r\na@b.c,Иван,100\r\nd@e.f,Марья,50\r\n""")

    def test_quotes_in_content(self):
        """При переводе xlsx в csv используется обрамление строк посредством '"'.

        Поэтому при оформлении текстовых значений можно использовать double-quoting:
        https://docs.python.org/2/library/csv.html#csv.Dialect.doublequote

        При переводе xlsx -> csv, для ячеек с текстовыми значениями, содержащими '"'
        мы должны увидеть задвоенные '"'.

        """

        with _get_data_file("xls/quotes.xlsx") as f:
            xls = XLSFile(f.read())

        correct_csv = """text\r\n"This text has ""quoted"" word, indeed."\r\n"""
        csv = xls.sheet_as_csv(sheet_name=0)

        self.assertEqual(csv, correct_csv)
