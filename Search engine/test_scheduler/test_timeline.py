from search.morty.tests.utils.test_case import MortyTestCase

from search.morty.src.scheduler.timeline import TimeLine, TimeInterval, merge_intervals


class TestTimeLine(MortyTestCase):
    def test_min(self):
        timeline = TimeLine()
        assert timeline.min() is None
        assert timeline.min(default=10) == 10

        timeline = TimeLine(start=0, end=10)
        assert timeline.min(after=-1) == 0
        assert timeline.min(after=5) == 6
        assert timeline.min(after=10) is None

        timeline = TimeLine()
        timeline._timeline.update((TimeInterval(start=0, end=10), TimeInterval(start=12, end=15)))
        assert timeline.min(after=11) == 12

    def test_max(self):
        timeline = TimeLine()
        assert timeline.max() is None
        assert timeline.max(default=10) == 10

        timeline = TimeLine(start=0, end=10)
        assert timeline.max(after=0) == 10
        assert timeline.max(after=9) == 10
        assert timeline.max(after=10) is None

        timeline = TimeLine()
        timeline._timeline.update((TimeInterval(start=0, end=10), TimeInterval(start=12, end=15)))
        assert timeline.max(after=10) == 15

    def test_shift(self):
        timeline = TimeLine()
        assert timeline.shift(0, 0) == timeline

        timeline = TimeLine(1, 5)
        assert timeline.shift(2, 2) == TimeLine(3, 7)

        timeline = TimeLine()
        timeline._timeline.update((TimeInterval(1, 5), TimeInterval(7, 10)))
        assert timeline.shift(3, 6) == TimeLine(4, 16)

        assert TimeLine(1, 3).shift(0, -4) == TimeLine()

    def test_merge_intervals(self):
        timeline = TimeLine()
        timeline._timeline.update((
            TimeInterval(-6, -6),

            TimeInterval(-4, -2),
            TimeInterval(-1, 0),

            TimeInterval(2, 4),
            TimeInterval(4, 6),

            TimeInterval(8, 10),
            TimeInterval(9, 12),

            TimeInterval(14, 18),
            TimeInterval(15, 16),

            TimeInterval(20, 22),
            TimeInterval(20, 22),
        ))

        res = TimeLine()
        res._timeline.update((
            TimeInterval(-6, -6),

            TimeInterval(-4, -2),
            TimeInterval(-1, 0),

            TimeInterval(2, 6),

            TimeInterval(8, 12),

            TimeInterval(14, 18),

            TimeInterval(20, 22),
        ))

        test = TimeLine()
        # fixme:
        test._timeline.update(merge_intervals(timeline))
        assert test == res

    def test_intersect(self):
        timeline = TimeLine()
        timeline._timeline.update((
            TimeInterval(-8, -7),
            TimeInterval(0, 10),
            TimeInterval(14, 20),
            TimeInterval(22, 27),
            TimeInterval(30, 33),

            TimeInterval(35, 37),
            TimeInterval(40, 50),
            TimeInterval(54, 60),
            TimeInterval(62, 67),

            TimeInterval(70, 80),
            TimeInterval(84, 90),
            TimeInterval(92, 97),

            TimeInterval(100, 110),
            TimeInterval(114, 120),

            TimeInterval(130, 140),
        ))

        other = TimeLine()
        other._timeline.update((
            TimeInterval(-9, -9),   # (x[0] - 1, x[0] - 1)
            TimeInterval(-1, 0),    # (x[0] - 1, x[0])
            TimeInterval(13, 15),   # (x[0] - 1, x[0] + 1)
            TimeInterval(21, 27),   # (x[0] - 1, x[1])
            TimeInterval(29, 34),   # (x[0] - 1, x[1] + 1)

            TimeInterval(35, 35),   # (x[0], x[0])
            TimeInterval(40, 41),   # (x[0], x[0] + 1)
            TimeInterval(54, 60),   # (x[0], x[1])
            TimeInterval(62, 68),   # (x[0], x[1] + 1)

            TimeInterval(71, 71),   # (x[0] + 1, x[0] + 1)
            TimeInterval(85, 90),   # (x[0] + 1, x[1])
            TimeInterval(93, 98),   # (x[0] + 1, x[1] + 1)

            TimeInterval(110, 110),     # (x[1], x[1])
            TimeInterval(120, 121),     # (x[1], x[1] + 1)

            TimeInterval(141, 141),     # (x[1] + 1, x[1] + 1)
        ))

        res = TimeLine()
        res._timeline.update((
            # None
            TimeInterval(0, 0),
            TimeInterval(14, 15),
            TimeInterval(22, 27),
            TimeInterval(30, 33),

            TimeInterval(35, 35),
            TimeInterval(40, 41),
            TimeInterval(54, 60),
            TimeInterval(62, 67),

            TimeInterval(71, 71),
            TimeInterval(85, 90),
            TimeInterval(93, 97),

            TimeInterval(110, 110),
            TimeInterval(120, 120),
            # None
        ))

        assert timeline.intersect([other]) == res

    def test_difference(self):
        timeline = TimeLine()
        timeline._timeline.update((
            TimeInterval(-8, -7),
            TimeInterval(0, 10),
            TimeInterval(14, 20),
            TimeInterval(22, 27),
            TimeInterval(30, 33),

            TimeInterval(35, 37),
            TimeInterval(40, 50),
            TimeInterval(54, 60),
            TimeInterval(62, 67),

            TimeInterval(70, 80),
            TimeInterval(84, 90),
            TimeInterval(92, 97),

            TimeInterval(100, 110),
            TimeInterval(114, 120),

            TimeInterval(130, 140),
        ))

        other = TimeLine()
        other._timeline.update((
            TimeInterval(-9, -9),  # (x[0] - 1, x[0] - 1)
            TimeInterval(-1, 0),  # (x[0] - 1, x[0])
            TimeInterval(13, 15),  # (x[0] - 1, x[0] + 1)
            TimeInterval(21, 27),  # (x[0] - 1, x[1])
            TimeInterval(29, 34),  # (x[0] - 1, x[1] + 1)

            TimeInterval(35, 35),  # (x[0], x[0])
            TimeInterval(40, 41),  # (x[0], x[0] + 1)
            TimeInterval(54, 60),  # (x[0], x[1])
            TimeInterval(62, 68),  # (x[0], x[1] + 1)

            TimeInterval(71, 71),  # (x[0] + 1, x[0] + 1)
            TimeInterval(85, 90),  # (x[0] + 1, x[1])
            TimeInterval(93, 98),  # (x[0] + 1, x[1] + 1)

            TimeInterval(110, 110),  # (x[1], x[1])
            TimeInterval(120, 121),  # (x[1], x[1] + 1)

            TimeInterval(141, 141),  # (x[1] + 1, x[1] + 1)
        ))

        res = TimeLine()
        res._timeline.update((
            TimeInterval(-8, -7),
            TimeInterval(1, 10),
            TimeInterval(16, 20),
            # None
            # None

            TimeInterval(36, 37),
            TimeInterval(42, 50),
            # None
            # None

            TimeInterval(70, 70), TimeInterval(72, 80),
            TimeInterval(84, 84),
            TimeInterval(92, 92),

            TimeInterval(100, 109),
            TimeInterval(114, 119),

            TimeInterval(130, 140),
        ))

        assert timeline.difference([other]) == res

    # fixme
    # def test_step(self):
        # pass
