def test_bean_bag_imports_beans_properly():
    from .beans import my_bean
    assert my_bean.square(5) == 25
    assert my_bean[5] == 25
