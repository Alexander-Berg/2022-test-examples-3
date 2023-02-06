from market.idx.pylibrary.report_control import auto_subzero


def test_disable(zk):
    # act
    auto_subzero.disable(zk_client=zk)

    # assert
    assert not auto_subzero.is_enabled(zk_client=zk)


def test_enable(zk):
    # act
    auto_subzero.enable(zk_client=zk)

    # assert
    assert auto_subzero.is_enabled(zk_client=zk)


def test_disable_dry_run(zk):
    # arrange
    auto_subzero.enable(zk_client=zk)

    # act
    auto_subzero.disable(zk_client=zk, dry_run=True)

    # assert
    assert auto_subzero.is_enabled(zk_client=zk)


def test_enable_dry_run(zk):
    # arrange
    auto_subzero.disable(zk_client=zk)

    # act
    auto_subzero.enable(zk_client=zk, dry_run=True)

    # assert
    assert not auto_subzero.is_enabled(zk_client=zk)
