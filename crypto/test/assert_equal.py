def assert_equal(left, right):
    assert left == right, (left, right)
    assert right == left, (left, right)
    assert not (left != right), (left, right)
    assert not (right != left), (left, right)


def assert_unequal(left, right):
    assert left != right, (left, right)
    assert right != left, (left, right)
    assert not (left == right), (left, right)
    assert not (right == left), (left, right)
