from hamcrest import assert_that, close_to

from market.pylibrary.mindexerlib.razladka import detect_razladka, Config, Result, TimeseriesConfig


def test_trivial():
    assert not detect_razladka([1, 1, 1, 1])
    assert detect_razladka([1, 1, 1, 2])


def test_detailed_result():
    razladka = detect_razladka([10, 11, 9, 12])

    assert razladka
    assert razladka.moving_avg == 10.25
    assert_that(razladka.moving_std, close_to(0.35, delta=0.01))
    assert razladka.median == 10
    assert razladka.crit == 1
    assert razladka.last == 12


def test_custom_confidence():
    config = Config(confidence=10)

    assert not detect_razladka([10, 11, 9, 12], config=config)


def test_timeseries():
    data = [
        ('2020-05-24 21:03:40', 1590354220, 6796980224),
        ('2020-05-24 22:48:54', 1590360534, 6821654528),
        ('2020-05-25 00:34:17', 1590366857, 6822678528),
        ('2020-05-25 02:18:02', 1590373082, 6824681472),
        ('2020-05-25 04:01:06', 1590379266, 6874873856),
        ('2020-05-25 05:45:51', 1590385551, 6892273664),
        ('2020-05-25 07:11:06', 1590390666, 6900916224),
        ('2020-05-25 08:39:15', 1590395955, 6858489856),
        ('2020-05-25 10:04:56', 1590401096, 6852087808),
        ('2020-05-25 11:29:46', 1590406186, 6856142848),
        ('2020-05-25 12:57:10', 1590411430, 7512309760),
        ('2020-05-25 14:25:46', 1590416746, 7145037824),
        ('2020-05-25 15:47:52', 1590421672, 6904598528),
        ('2020-05-25 17:15:24', 1590426924, 6867808256),
        ('2020-05-25 18:40:37', 1590432037, 6888824832),
        ('2020-05-25 20:03:35', 1590437015, 6913142784),
        ('2020-05-25 21:39:10', 1590442750, 6908747776),
        ('2020-05-25 23:03:30', 1590447810, 6910889984),
        ('2020-05-26 00:37:50', 1590453470, 6921945088),
        ('2020-05-26 02:01:01', 1590458461, 6926311424),
        ('2020-05-26 04:49:06', 1590468546, 7015129088),
        ('2020-05-26 06:22:31', 1590474151, 7061311488),
        ('2020-05-26 07:45:10', 1590479110, 6978273280),
        ('2020-05-26 09:31:13', 1590485473, 6916296704),
        ('2020-05-26 10:59:52', 1590490792, 6907863040),
        ('2020-05-26 12:27:33', 1590496053, 6930182144),
    ]
    data = [(t[1], t[2]) for t in data]

    config = TimeseriesConfig(now=1590496055, smooth_window_size='12H', final_median_window_size='1D')

    razladka = detect_razladka(data, config=config)
    assert not razladka

    # moving_avg=6911433522.68, moving_std=70024774.3698
    # threshold_lo=210074323.109, threshold_hi=315111484.664
    data.append((1590496054, 7220000000))
    assert not detect_razladka(data, config=config)

    data.append((1590496055, 1000000000))
    assert detect_razladka(data, config=config)


def test_empty():
    assert not detect_razladka([])


def test_required_confidence():
    razladka = Result(moving_avg=10, moving_std=1, last=8.5)

    assert razladka.required_confidence == 2


def test_default_required_confidence():
    razladka = Result()

    assert razladka.required_confidence == 0


def test_threshold():
    assert not detect_razladka([1, 1, 1, 1.2], config=Config(threshold=0.3))


def test_one_element():
    assert not detect_razladka([1])


def test_two_elements():
    assert detect_razladka([1, 1.1])
