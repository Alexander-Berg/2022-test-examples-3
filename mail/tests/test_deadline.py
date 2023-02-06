from unittest.mock import patch

import pytest

from sendr_interactions.deadline import Deadline


@pytest.mark.parametrize(('deadline', 'expected'), [
    (0.1, -0.9),
    (10, 9),
    (0, -1),
    (-1, -2),
])
def test_deadline(deadline, expected, mocker):
    with patch('time.monotonic', return_value=0):
        d = Deadline(seconds=deadline)

    with patch('time.monotonic', return_value=1):
        assert d.seconds_to() == expected
