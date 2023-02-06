import threading

from edera import Flag
from edera.flagtories import InterThreadFlagtory


def test_flagtory_allocates_unraised_flags():
    flagtory = InterThreadFlagtory()
    flag = flagtory.allocate()
    assert isinstance(flag, Flag)
    assert not flag.raised


def test_flag_can_be_raised_from_another_thread():

    def raise_flag(flag):
        flag.up()

    flagtory = InterThreadFlagtory()
    flag = flagtory.allocate()
    raiser = threading.Thread(target=raise_flag, args=(flag,))
    raiser.daemon = True
    raiser.start()
    raiser.join()
    assert flag.raised


def test_flag_can_be_unraised_from_another_thread():

    def unraise_flag(flag):
        flag.down()

    flagtory = InterThreadFlagtory()
    flag = flagtory.allocate()
    flag.up()
    unraiser = threading.Thread(target=unraise_flag, args=(flag,))
    unraiser.daemon = True
    unraiser.start()
    unraiser.join()
    assert not flag.raised
