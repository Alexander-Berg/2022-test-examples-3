import blackbox
import _repr
import unittest
import difflib
import lxml


def _decompose_argument(arg):
    if isinstance(arg, blackbox.BlackboxResponse):
        return arg.get_value(), arg.get_message()
    return arg, None


def _xmlElementToString(element, original_response):
    return blackbox.BlackboxResponse(
        lxml.etree.tostring(lxml.etree.ElementTree(element)), "xml element to string", original_response
    )


class TestCaseException(AssertionError):
    """Exception of blackbox test."""

    def __init__(self, *info_seq):
        message = '\n'.join(filter(None, [_decompose_argument(info)[1] or info for info in info_seq])).strip()
        message = message.replace('\n', '\n>  ')
        AssertionError.__init__(self, message)


def _is_string(arg):
    return isinstance(arg, basestring)


def _is_collection(arg):
    return hasattr(arg, '__contains__') or hasattr(arg, '__iter__') or hasattr(arg, '__getitem__')


class TestCase(unittest.TestCase):
    """Wrapper for the unittest.TestCase class.

    Wrapper for the unittest.TestCase class. It extends the list of supported assertions.
    """

    def failure(self, *message):
        raise TestCaseException(*message)

    def assertTrue(self, first, message=None):
        first_value, first_message = _decompose_argument(first)
        if not first_value:
            self.failure(message or u'%s not True' % _repr.safe_repr(first_value), first_message)

    def assertFalse(self, first, message=None):
        first_value, first_message = _decompose_argument(first)
        if first_value:
            self.failure(message or u'%s not False' % _repr.safe_repr(first_value), first_message)

    def assertEqual(self, first, second, message=None):
        first_value, first_message = _decompose_argument(first)
        second_value, second_message = _decompose_argument(second)
        # special casses of strings and xml-elements lists
        if isinstance(first_value, basestring):
            return self.assertStringEqual(first, second, message)
        if isinstance(first_value, list):
            return self.assertListEqual(first, second, message)
        if isinstance(first_value, float):
            return self.assertFloatEqual(first, second, message)
        if type(first_value) is lxml.etree._Element:
            return self.assertXmlEqual(first, second, message)

        if not first_value == second_value:
            self.failure(
                message or u'%s not equal to %s' % (_repr.safe_repr(first_value), _repr.safe_repr(second_value)),
                first_message,
                second_message,
            )

    def assertFloatEqual(self, first, second, message=None, rel_tol=1e-6, abs_tol=1e-6):
        first_value, first_message = _decompose_argument(first)
        second_value, second_message = _decompose_argument(second)

        # same as in math.is_close in python 3.5
        if not (abs(first_value - second_value) <= max(rel_tol * max(abs(first_value), abs(second_value)), abs_tol)):
            self.failure(
                message or u'%s not equal to %s' % (_repr.safe_repr(first_value), _repr.safe_repr(second_value)),
                first_message,
                second_message,
            )

    def assertStringEqual(self, first, second, message=None):
        first_value, first_message = _decompose_argument(first)
        second_value, second_message = _decompose_argument(second)
        for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(
            None, unicode(first_value), unicode(second_value)
        ).get_opcodes():
            if tag != 'equal':
                self.failure(
                    message
                    or u'%s not equal to %s. Start on %d'
                    % (_repr.safe_repr(first_value[i1:i2]), _repr.safe_repr(second_value[j1:j2]), i1),
                    first_message,
                    second_message,
                )

    def assertStringEqualUnified(self, first, second, message=None):
        first = _repr.unify_spaces(unicode(first))
        second = _repr.unify_spaces(unicode(second))
        return self.assertStringEqual(first, second, message)

    def assertListEqual(self, first, second, message=None):
        first_value, first_message = _decompose_argument(first)
        second_value, second_message = _decompose_argument(second)
        if not (isinstance(first_value, list) and isinstance(second_value, list)):
            self.failure(message or u'trying to compare non-lists as lists', first_message, second_message)
        first_len = len(first_value)
        second_len = len(second_value)
        if first_len != second_len:
            self.failure(
                message or u'length different: %d and %d' % (first_len, second_len), first_message, second_message
            )
        element_index = 0
        first_response = first if isinstance(first, blackbox.BlackboxResponse) else None
        second_response = second if isinstance(second, blackbox.BlackboxResponse) else None
        for first_head, second_head in zip(first_value, second_value):
            self.assertEqual(
                blackbox.BlackboxResponse(first_head, "%d-th element" % element_index, first_response),
                blackbox.BlackboxResponse(second_head, "%d-th element" % element_index, second_response),
                message,
            )
            element_index = element_index + 1
        return

    def assertXmlEqual(self, first, second, message=None):
        first_value, first_message = _decompose_argument(first)
        second_value, second_message = _decompose_argument(second)
        self.assertStringEqual(
            _xmlElementToString(first_value, first), _xmlElementToString(second_value, second), message
        )

    def assertNotEqual(self, first, second, message=None):
        first_value, first_message = _decompose_argument(first)
        second_value, second_message = _decompose_argument(second)
        if first_value == second_value:
            self.failure(
                message or u'%s equal to %s' % (_repr.safe_repr(first_value), _repr.safe_repr(second_value)),
                first_message,
                second_message,
            )

    def assertLess(self, first, second, message=None):
        first_value, first_message = _decompose_argument(first)
        second_value, second_message = _decompose_argument(second)
        if not first_value < second_value:
            self.failure(
                message or u'%s not less than %s' % (_repr.safe_repr(first_value), _repr.safe_repr(second_value)),
                first_message,
                second_message,
            )

    def assertGreater(self, first, second, message=None):
        first_value, first_message = _decompose_argument(first)
        second_value, second_message = _decompose_argument(second)
        if not first_value > second_value:
            self.failure(
                message or u'%s not greater than %s' % (_repr.safe_repr(first_value), _repr.safe_repr(second_value)),
                first_message,
                second_message,
            )

    def assertZero(self, first, message=None):
        first_value, first_message = _decompose_argument(first)
        if first_value != 0:
            self.failure(message or u'%s not zero' % (_repr.safe_repr(first_value)), first_message)

    def assertPositive(self, first, message=None):
        first_value, first_message = _decompose_argument(first)
        if not first_value > 0:
            self.failure(message or u'%s not positive' % (_repr.safe_repr(first_value)), first_message)

    def assertIn(self, element, collection, message=None):
        element_value, element_message = _decompose_argument(element)
        collection_value, collection_message = _decompose_argument(collection)
        if _is_string(collection_value) or not _is_collection(collection_value):
            self.failure(message or u'%s not collection' % (_repr.safe_repr(collection_value)), collection_message)
        if element_value not in collection_value:
            self.failure(
                message or u'%s not found in %s' % (_repr.safe_repr(element_value), _repr.safe_repr(collection_value)),
                element_message,
                collection_message,
            )

    def assertNotIn(self, element, collection, message=None):
        element_value, element_message = _decompose_argument(element)
        collection_value, collection_message = _decompose_argument(collection)
        if _is_string(collection_value) or not _is_collection(collection_value):
            self.failure(message or u'%s not collection' % (_repr.safe_repr(collection_value)), collection_message)
        if element_value in collection_value:
            self.failure(
                message or u'%s not found in %s' % (_repr.safe_repr(element_value), _repr.safe_repr(collection_value)),
                element_message,
                collection_message,
            )

    def assertSubstring(self, string, collection, message=None):
        string_value, string_message = _decompose_argument(string)
        collection_value, collection_message = _decompose_argument(collection)
        if not _is_string(string_value):
            self.failure(message or u'%s not string' % (_repr.safe_repr(string_value)), string_message)
        if _is_string(collection_value):
            if string_value in collection_value:
                return  # OK
        elif _is_collection(collection_value):
            for member in collection_value:
                if _is_string(member):
                    if string_value in member:
                        return  # OK
                else:
                    self.failure(message or u'%s not string' % (_repr.safe_repr(member)), collection_message)
        else:
            self.failure(
                message or u'%s not string or collection' % (_repr.safe_repr(collection_value)), collection_message
            )
        self.failure(
            message or u'%s not found in %s' % (_repr.safe_repr(string_value), _repr.safe_repr(collection_value)),
            string_message,
            collection_message,
        )

    def assertSubstringUnified(self, string, collection, message=None):
        string = _repr.unify_spaces(unicode(string))
        collection = _repr.unify_spaces(unicode(collection))
        return self.assertSubstring(string, collection, message)

    def assertNotSubstring(self, string, collection, message=None):
        string_value, string_message = _decompose_argument(string)
        collection_value, collection_message = _decompose_argument(collection)
        if not _is_string(string_value):
            self.failure(message or u'%s not string' % (_repr.safe_repr(string_value)), string_message)
        if _is_string(collection_value):
            if string_value in collection_value:
                self.failure(
                    message
                    or u'%s unexpectedly found in %s'
                    % (_repr.safe_repr(string_value), _repr.safe_repr(collection_value)),
                    string_message,
                    collection_message,
                )
        elif _is_collection(collection_value):
            for member in collection_value:
                if _is_string(member):
                    if string_value in member:
                        self.failure(
                            message
                            or u'%s unexpectedly found in %s'
                            % (_repr.safe_repr(string_value), _repr.safe_repr(collection_value)),
                            string_message,
                            collection_message,
                        )
                else:
                    self.failure(message or u'%s not string' % (_repr.safe_repr(member)), collection_message)
        else:
            self.failure(
                message or u'%s not string or collection' % (_repr.safe_repr(collection_value)), collection_message
            )

    def _assertIsSortedOrNot(self, iterable, reverse, message, key, should_be_sorted):
        iterable_value, iterable_message = _decompose_argument(iterable)

        sorted_iterable_value = sorted(iterable_value, key=key, reverse=reverse)
        if should_be_sorted:
            if iterable_value != sorted_iterable_value:
                raise self.failureException(
                    message or u'%s is not sorted' % _repr.safe_repr(iterable_value), iterable_message
                )
        else:
            if iterable_value == sorted_iterable_value:
                raise self.failureException(
                    message or u'%s is sorted' % _repr.safe_repr(iterable_value), iterable_message
                )

    def assertIsSorted(self, iterable, reverse=False, message=None, key=None):
        self._assertIsSortedOrNot(iterable, reverse, message, key, True)

    def assertIsNotSorted(self, iterable, reverse=False, message=None, key=None):
        self._assertIsSortedOrNot(iterable, reverse, message, key, False)


def run_test_case(testCase):
    """Test case launcher.

    Helper for a test case launching.
    """
    assert issubclass(testCase, TestCase)

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(testCase)
    result = unittest.TextTestRunner().run(suite)

    return result.wasSuccessful()


def run_and_exit(testCase):
    """Test case launcher.

    Helper for a usual case: exit()'s with a proper return code.
    """
    ok = run_test_case(testCase)
    exit(not ok)
