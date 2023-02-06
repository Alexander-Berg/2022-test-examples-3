import multiprocessing

from edera import Flag
from edera.flagtories import InterProcessFlagtory


def test_flagtory_allocates_unraised_flags():
    flagtory = InterProcessFlagtory()
    flag = flagtory.allocate()
    assert isinstance(flag, Flag)
    assert not flag.raised


def test_flag_can_be_raised_from_another_process():

    def raise_flag(flag):
        flag.up()

    flagtory = InterProcessFlagtory()
    flag = flagtory.allocate()
    raiser = multiprocessing.Process(target=raise_flag, args=(flag,))
    raiser.daemon = True
    raiser.start()
    raiser.join()
    assert flag.raised


def test_flag_can_be_unraised_from_another_process():

    def unraise_flag(flag):
        flag.down()

    flagtory = InterProcessFlagtory()
    flag = flagtory.allocate()
    flag.up()
    unraiser = multiprocessing.Process(target=unraise_flag, args=(flag,))
    unraiser.daemon = True
    unraiser.start()
    unraiser.join()
    assert not flag.raised
