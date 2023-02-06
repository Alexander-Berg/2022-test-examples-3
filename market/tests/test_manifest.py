# coding: utf-8

import pytest

import json
import shutil
from pathlib import Path
from schema.schema import SchemaError
from market.sre.tools.dpreparer.lib.models.manifest import Manifest
from market.sre.tools.dpreparer.lib.sections.dummy import DummySection


def test_init_manifest(fxt_manifest, fxt_json_manifest):
    assert fxt_json_manifest == Manifest(fxt_manifest).content


def test_mfst_validate_exception(fxt_manifest_err):
    with pytest.raises(SchemaError) as _:
        Manifest(fxt_manifest_err).validate()


def test_validate_manifest(fxt_manifest):
    manifest = Manifest(fxt_manifest)
    assert manifest.block_names == [
        'application',
        'additional',
        'first_include',
        'second_include'
    ]

    test_block = manifest.get_block('application')
    assert test_block.name == 'application'
    assert test_block.dirs.data == [
        {'name': 'output_dir/test1'},
        {'name': 'output_dir/test2'}
    ]
    assert test_block.links.data == [
        {'name': 'output_dir/test3', 'target': 'output_dir/test1'}
    ]
    assert test_block.templater.data == {
        'destination': 'output_dir/test',
        'source': 'test_templates'
    }

    assert [n.name for n in manifest.blocks] == [
        'application',
        'additional',
        'first_include',
        'second_include'
    ]


def test_mfst_minimal(fxt_manifest_mininal):
    manifest = Manifest(fxt_manifest_mininal)
    test_block = manifest.get_block('minimal_example')
    assert test_block.name == 'minimal_example'
    assert test_block.dirs.data == []
    assert test_block.links.data == []
    assert test_block.templater.data == []


def test_dirsection(fxt_manifest):
    manifest = Manifest(fxt_manifest)

    for block in manifest.blocks:
        block.dirs.execute()

        for d in block.dirs.data:
            p = Path(d['name'])
            assert p.exists()
            assert p.is_dir()


def test_filesection(fxt_manifest):
    manifest = Manifest(fxt_manifest)

    for block in manifest.blocks:
        block.files.execute()

        for d in block.files.data:
            p = Path(d['name'])
            assert p.exists()
            assert p.is_file()


def test_linkssection(fxt_manifest):
    manifest = Manifest(fxt_manifest)

    for block in manifest.blocks:
        block.links.execute()

        for d in block.links.data:
            p = Path(d['name'])
            assert p.is_symlink()


def test_dummy_sections(fxt_manifest_mininal):
    manifest = Manifest(fxt_manifest_mininal)
    assert all([isinstance(s, DummySection) for block in manifest.blocks for s in block.sections])


def test_split_secrets_section(fxt_manifest, meta_secret_file):
    manifest = Manifest(fxt_manifest)
    for block in manifest.blocks:
        for d in block.split_secrets.data:
            shutil.copy(meta_secret_file, d['name'])
        block.split_secrets.execute()

        for d in block.split_secrets.data:
            data = json.loads(Path(d['name']).read_text())
            for filename, content in data.items():
                path = Path('output_dir', filename)
                assert path.is_file()
                assert content == path.read_text()
