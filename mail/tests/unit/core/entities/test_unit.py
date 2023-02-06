from mail.beagle.beagle.core.entities.unit import Unit


def test_get_external_key(rands):
    type_ = rands()
    id_ = rands()
    assert Unit.get_external_key(type_, id_) == (type_, id_)


def test_external_key(randn, rands):
    unit = Unit(
        org_id=randn(),
        external_id=rands(),
        external_type=rands(),
        name=rands(),
    )
    assert unit.external_key == (unit.external_type, unit.external_id)
