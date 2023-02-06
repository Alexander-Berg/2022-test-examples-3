# coding: utf-8

from tests_common.pytest_bdd import then


@then(u'serial "{serial_type:w}" is "{expected:d}"')
def step_check_next_serial(context, serial_type, expected):
    real_serials = context.qs.serials()
    try:
        real = getattr(real_serials, serial_type)
    except AttributeError as exc:
        raise AttributeError(
            'Strange serial type %r : %s' % (serial_type, exc))
    assert expected == real, \
        'Expect %d got %r as %s ' % (
            expected, real, serial_type)
