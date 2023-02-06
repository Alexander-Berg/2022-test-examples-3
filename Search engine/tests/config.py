import logging
import textwrap

import py
import pytest

import library.config


def test_parser(tmpdir):
    conf = tmpdir.join('test.yaml')
    conf.write(textwrap.dedent(
        """
        one:
            two:
                three:
                    - list_item1
                    - list_item2
                    - list_item3
                dict_item: some
        """
    ))

    cache = library.config.RegistryBase(logging.getLogger()).load(conf)
    assert isinstance(cache, library.config.RegistryBase.Cache)
    assert cache == library.config.RegistryBase(None).load(conf)

    with pytest.raises(py.error.Error):
        library.config.RegistryBase(None).load(tmpdir.join('notexists_path.yaml'))

    assert isinstance(cache.data, dict)
    conf = library.config.RegistryBase(None).query(cache.data)
    assert isinstance(conf, library.config.RegistryBase.Config)
    assert isinstance(conf.one, library.config.RegistryBase.Config)
    assert isinstance(conf.one['two'], dict)
    assert isinstance(conf.one.two.three, list)
    assert len(conf.one.two.three) == 3
    assert conf.one.two.dict_item == 'some'
