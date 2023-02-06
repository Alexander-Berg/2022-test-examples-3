from mail.husky.stages.worker.logic.signal_history import SignalHistory


N_STEPS = 200

LOW_LOAD_VALUE = 0.1
MEDIUM_LOAD_VALUE = 0.7
HIGH_LOAD_VALUE = 0.9


def low_load_after_high_load_generator(n_steps=N_STEPS):
    for i in range(n_steps // 2):
        yield HIGH_LOAD_VALUE
    for i in range(n_steps - (n_steps // 2)):
        yield LOW_LOAD_VALUE


def medium_load_after_high_load_generator(n_steps=N_STEPS):
    for i in range(n_steps // 2):
        yield HIGH_LOAD_VALUE
    for i in range(n_steps - (n_steps // 2)):
        yield MEDIUM_LOAD_VALUE


def medium_load_after_low_load_generator(n_steps=N_STEPS):
    for i in range(n_steps // 2):
        yield LOW_LOAD_VALUE
    for i in range(n_steps - (n_steps // 2)):
        yield MEDIUM_LOAD_VALUE


def test_low_load():
    signal = SignalHistory(0.5, 0.85)

    for i in range(N_STEPS):
        signal.update_history(LOW_LOAD_VALUE)

    assert not signal.is_overloaded()
    assert signal.is_underloaded()


def test_high_load():
    signal = SignalHistory(0.5, 0.85)

    for i in range(N_STEPS):
        signal.update_history(HIGH_LOAD_VALUE)

    assert signal.is_overloaded()
    assert not signal.is_underloaded()


def test_low_load_after_high_load():
    signal = SignalHistory(0.5, 0.85)

    for i in range(N_STEPS // 2):
        signal.update_history(HIGH_LOAD_VALUE)
    assert signal.is_overloaded()
    for i in range(N_STEPS - (N_STEPS // 2)):
        signal.update_history(LOW_LOAD_VALUE)
    assert signal.is_underloaded()
