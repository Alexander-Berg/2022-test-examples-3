# -*- coding: utf-8 -*-
import pytest

from rtcc.core.dataobjects import ConfigResult
from rtcc.core.dataobjects import ConfigResultList
from rtcc.core.dataobjects import ConfigurationId
from rtcc.core.dataobjects import ConfigurationInfo
from rtcc.core.dataobjects import DiffResult
from rtcc.core.dataobjects import DiffResultList
from rtcc.core.dataobjects import DnsDiff
from rtcc.core.dataobjects import FileInfo
from rtcc.core.dataobjects import FileInfoList
from rtcc.core.dataobjects import GeneratorResult
from rtcc.core.dataobjects import GeneratorResultList
from rtcc.core.dataobjects import PatchInfo
from rtcc.core.dataobjects import SessionDiff
from rtcc.core.dataobjects import TestInfo as CoreTestInfo
from rtcc.core.dataobjects import TestInfoList as CoreTestInfoList
from rtcc.core.dataobjects import TopologyDiff


def test_testresult_to_json():
    tr = CoreTestInfo("test_name",
                  "Error Message",
                  "Error message description.\n stackrace or any other test data",
                  "FAIL")
    assert tr.to_json() == {"name": "test_name",
                            "output": "Error message description.\n stackrace or any other test data",
                            "message": "Error Message",
                            "status": "FAIL"}


def test_testresult_from_json():
    tr = CoreTestInfo.from_json({"name": "test_name",
                             "output": "Error message description.\n stackrace or any other test data",
                             "message": "Error Message",
                             "status": "FAIL"})

    assert tr.name == "test_name"
    assert tr.message == "Error Message"
    assert tr.output == "Error message description.\n stackrace or any other test data"
    assert tr.status == "FAIL"


def test_fileresult_to_json():
    fr = FileInfo("file.name", "line 1\nline 2\nline 3\nline 4\n")
    assert fr.to_json() == {"name": "file.name",
                            "content": "line 1\nline 2\nline 3\nline 4\n"}


def test_fileresult_from_json():
    fr = FileInfo.from_json({"name": "file.name",
                             "content": "line 1\nline 2\nline 3\nline 4\n"})
    assert fr.name == "file.name"
    assert fr.content == "line 1\nline 2\nline 3\nline 4\n"


def test_configurationid_to_json():
    fr = ConfigurationId("contour", "stand", "location")
    assert fr.to_json() == {"contour": "contour",
                            "stand": "stand",
                            "location": "location"}


def test_configurationid_from_json():
    ci = ConfigurationId.from_json({"contour": "contour",
                                    "stand": "stand",
                                    "location": "location"})
    assert ci.contour == "contour"
    assert ci.stand == "stand"
    assert ci.location == "location"


def test_configurationinfo_to_json():
    conf_id = ConfigurationId("contour", "stand", "location")
    conf_info = ConfigurationInfo(conf_id, "HEAD")
    assert conf_info.to_json() == {"revision": "HEAD",
                                   "config_id": {"contour": "contour",
                                                 "stand": "stand",
                                                 "location": "location"}}


def test_configurationinfo_from_json():
    conf_info = ConfigurationInfo.from_json({"revision": "HEAD",
                                             "config_id": {"contour": "contour",
                                                           "stand": "stand",
                                                           "location": "location"}})
    assert conf_info.revision == "HEAD"
    assert conf_info.config_id.contour == "contour"
    assert conf_info.config_id.stand == "stand"
    assert conf_info.config_id.location == "location"


def test_generationresults_to_json():
    file1 = FileInfo("file1.name", "line 1\nline 2\nline 3\nline 4\n")
    file2 = FileInfo("file2.name", "line 1\nline 2\n")
    tr1 = CoreTestInfo("test_name1", "", "", "SUCCESS")
    list_res = CoreTestInfoList([tr1])
    file_res = FileInfoList([file1, file2])
    gen_res = GeneratorResult("custom", file_res, list_res)
    gen_res.results.add_test_success("test_name2")
    gen_res.results.add_test_fail("test_name3", "ErrorMessage")
    gen_res.results.add_test_warning("test_name4", "WarnMessage", "simple warning")
    gen_res.results.add_test_error("test_name5", "ErrorMessage", "another error")
    assert gen_res.to_json() == {
        'generator': 'custom',
        'files': [
            {'content': 'line 1\nline 2\nline 3\nline 4\n', 'name': 'file1.name'},
            {'content': 'line 1\nline 2\n', 'name': 'file2.name'}
        ],
        'results': [
            {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name1'},
            {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name2'},
            {'status': 'FAILED', 'output': '', 'message': 'ErrorMessage', 'name': 'test_name3'},
            {'status': 'WARNING', 'output': 'simple warning', 'message': 'WarnMessage', 'name': 'test_name4'},
            {'status': 'ERROR', 'output': 'another error', 'message': 'ErrorMessage', 'name': 'test_name5'}
        ]
    }


def test_generationresults_from_json():
    gen_res = GeneratorResult.from_json({
        'generator': 'custom',
        'files': [
            {'content': 'line 1\nline 2\nline 3\nline 4\n', 'name': 'file1.name'},
            {'content': 'line 1\nline 2\n', 'name': 'file2.name'}
        ],
        'results': [
            {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name1'},
            {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name2'},
            {'status': 'FAILED', 'output': '', 'message': 'ErrorMessage', 'name': 'test_name3'},
            {'status': 'WARNING', 'output': 'simple warning', 'message': 'WarnMessage', 'name': 'test_name4'},
            {'status': 'ERROR', 'output': 'another error', 'message': 'ErrorMessage', 'name': 'test_name5'}
        ]
    })
    assert len(gen_res.files.list) == 2
    assert len(gen_res.results.list) == 5
    assert len(gen_res.results.success) == 2
    assert len(gen_res.results.failed) == 1
    assert gen_res.results.failed[0].name == "test_name3"
    assert len(gen_res.results.warnings) == 1
    assert gen_res.results.warnings[0].name == "test_name4"
    assert len(gen_res.results.errors) == 1
    assert gen_res.results.errors[0].name == "test_name5"


def test_generationresultslist_to_json():
    file1 = FileInfo("file1.name", "line 1\nline 2\nline 3\nline 4\n")
    file2 = FileInfo("file2.name", "line 1\nline 2\n")
    tr1 = CoreTestInfo("test_name1", "", "", "SUCCESS")
    list_res = CoreTestInfoList([tr1])
    file_res = FileInfoList([file1, file2])
    gen_res = GeneratorResult('custom', file_res, list_res)
    gen_res_list = GeneratorResultList([gen_res, gen_res])
    assert gen_res_list.to_json() == [{
        'generator': 'custom',
        'files': [
            {'content': 'line 1\nline 2\nline 3\nline 4\n', 'name': 'file1.name'},
            {'content': 'line 1\nline 2\n', 'name': 'file2.name'}
        ],
        'results': [
            {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name1'},
        ]
    }, {
        'generator': 'custom',
        'files': [
            {'content': 'line 1\nline 2\nline 3\nline 4\n', 'name': 'file1.name'},
            {'content': 'line 1\nline 2\n', 'name': 'file2.name'}
        ],
        'results': [
            {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name1'},
        ]
    }]


def test_generationresultslist_from_json():
    gen_res_list = GeneratorResultList.from_json([
        {
            'generator': 'custom',
            'files': [
                {'content': 'line 1\nline 2\nline 3\nline 4\n', 'name': 'file1.name'},
                {'content': 'line 1\nline 2\n', 'name': 'file2.name'}
            ],
            'results': [
                {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name1'},
                {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name2'},
                {'status': 'FAILED', 'output': '', 'message': 'ErrorMessage', 'name': 'test_name3'},
                {'status': 'WARNING', 'output': 'simple warning', 'message': 'WarnMessage', 'name': 'test_name4'},
                {'status': 'ERROR', 'output': 'another error', 'message': 'ErrorMessage', 'name': 'test_name5'}
            ]
        },
        {
            'generator': 'custom',
            'files': [
                {'content': 'line 1\nline 2\nline 3\nline 4\n', 'name': 'file1.name'},
                {'content': 'line 1\nline 2\n', 'name': 'file2.name'}
            ],
            'results': [
                {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name1'},
                {'status': 'SUCCESS', 'output': '', 'message': '', 'name': 'test_name2'},
                {'status': 'FAILED', 'output': '', 'message': 'ErrorMessage', 'name': 'test_name3'},
                {'status': 'WARNING', 'output': 'simple warning', 'message': 'WarnMessage', 'name': 'test_name4'},
                {'status': 'ERROR', 'output': 'another error', 'message': 'ErrorMessage', 'name': 'test_name5'}
            ]
        },
    ])
    assert len(gen_res_list.list) == 2
    assert len(gen_res_list.list[0].files.list) == 2
    assert len(gen_res_list.list[1].files.list) == 2


# SUCCESS = "SUCCESS"
# WARNING = "WARNING"
# FAILED = "FAILED"
# ERROR = "ERROR"

def test_patchinfo_to_json():
    prototype = ConfigurationId("contour", "stand", "location")
    patch_info = PatchInfo(prototype, {"source": {"hosts": "group"}})
    assert patch_info.to_json() == {
        'prototype': {"contour": "contour",
                      "stand": "stand",
                      "location": "location"},
        'data': {"source": {"hosts": "group"}}}


def test_patchinfo_from_json():
    patch_info = PatchInfo.from_json({
        'prototype': {"contour": "contour",
                      "stand": "stand",
                      "location": "location"},
        'data': {"source": {"hosts": "group"}}})
    assert patch_info.prototype.contour == "contour"
    assert patch_info.prototype.stand == "stand"
    assert patch_info.prototype.location == "location"
    assert patch_info.data == {"source": {"hosts": "group"}}


@pytest.mark.skip(reason="find the way to pass path to test")
def test_configinfolist_from_patch():
    import collections
    import json
    patch = json.load(open("tests/test_data/maestro_contours_info.json"),
                      encoding='utf-8',
                      object_pairs_hook=collections.OrderedDict)
    list = ConfigurationInfo.from_patch(patch, "")
    assert list[0].patch.prototype.contour == "production"


def test_configurationgenerationresult_from_json():
    gen_result = ConfigResult.from_json({
        'config_info': {
            'config_id': {
                'location': 'location',
                'contour': 'contour',
                'stand': 'stand'},
            'revision': 'HEAD'},
        'generation_result': []})
    assert gen_result.config_info.config_id.contour == "contour"
    assert gen_result.generation_result.list == []


def test_configurationgenerationresult_to_json():
    config_id = ConfigurationId("contour", "stand", "location")
    config_info = ConfigurationInfo(config_id, "HEAD")
    gen_result = ConfigResult(config_info, GeneratorResultList())
    assert gen_result.to_json() == {
        'config_info': {
            'config_id': {
                'location': 'location',
                'contour': 'contour',
                'stand': 'stand'},
            'revision': 'HEAD'},
        'generation_result': []}


def test_configurationgenerationresultlist_from_json():
    gen_result = ConfigResultList.from_json([{
        'config_info': {
            'config_id': {
                'location': 'location',
                'contour': 'contour',
                'stand': 'stand'},
            'revision': 'HEAD'},
        'generation_result': []}])
    assert len(gen_result.list) == 1


def test_configurationgenerationresultlist_to_json():
    config_id = ConfigurationId("contour", "stand", "location")
    config_info = ConfigurationInfo(config_id, "HEAD")
    gen_result = ConfigResult(config_info, GeneratorResultList())
    gen_results = ConfigResultList([gen_result])
    assert gen_results.to_json() == [{
        'config_info': {
            'config_id': {
                'location': 'location',
                'contour': 'contour',
                'stand': 'stand'},
            'revision': 'HEAD'},
        'generation_result': []}]


def test_diffresult_from_json():
    gen_result = DiffResult.from_json({
        'config_info_a': {
            'config_id': {
                'location': 'location_a',
                'contour': 'contour_a',
                'stand': 'stand_a'},
            'revision': 'HEAD'},
        'config_info_b': {
            'config_id': {
                'location': 'location_b',
                'contour': 'contour_b',
                'stand': 'stand_b'},
            'revision': 'HEAD'},
        'diff_result': []})
    assert gen_result.config_info_a.config_id.contour == "contour_a"
    assert gen_result.config_info_b.config_id.contour == "contour_b"
    assert gen_result.diff_result.list == []


def test_diffresult_to_json():
    config_id_a = ConfigurationId("contour_a", "stand_a", "location_a")
    config_info_a = ConfigurationInfo(config_id_a, "HEAD")
    config_id_b = ConfigurationId("contour_b", "stand_b", "location_b")
    config_info_b = ConfigurationInfo(config_id_b, "HEAD")
    diff_result = DiffResult(config_info_a, config_info_b, GeneratorResultList())
    assert diff_result.to_json() == {
        'config_info_a': {
            'config_id': {
                'location': 'location_a',
                'contour': 'contour_a',
                'stand': 'stand_a'},
            'revision': 'HEAD'},
        'config_info_b': {
            'config_id': {
                'location': 'location_b',
                'contour': 'contour_b',
                'stand': 'stand_b'},
            'revision': 'HEAD'},
        'diff_result': []}


def test_diffresultlist_from_json():
    gen_result = DiffResultList.from_json([{
        'config_info_a': {
            'config_id': {
                'location': 'location_a',
                'contour': 'contour_a',
                'stand': 'stand_a'},
            'revision': 'HEAD'},
        'config_info_b': {
            'config_id': {
                'location': 'location_b',
                'contour': 'contour_b',
                'stand': 'stand_b'},
            'revision': 'HEAD'},
        'diff_result': []}])
    assert len(gen_result.list) == 1


def test_diffresultlist_to_json():
    config_id_a = ConfigurationId("contour_a", "stand_a", "location_a")
    config_info_a = ConfigurationInfo(config_id_a, "HEAD")
    config_id_b = ConfigurationId("contour_b", "stand_b", "location_b")
    config_info_b = ConfigurationInfo(config_id_b, "HEAD")
    diff_result = DiffResult(config_info_a, config_info_b, GeneratorResultList())
    diff_results = ConfigResultList([diff_result])
    assert diff_results.to_json() == [{
        'config_info_a': {
            'config_id': {
                'location': 'location_a',
                'contour': 'contour_a',
                'stand': 'stand_a'},
            'revision': 'HEAD'},
        'config_info_b': {
            'config_id': {
                'location': 'location_b',
                'contour': 'contour_b',
                'stand': 'stand_b'},
            'revision': 'HEAD'},
        'diff_result': []}]


def test_session_diff_from_json():
    session_diff = SessionDiff.from_json({
        "config_diff":
            [{
                'config_info_a': {
                    'config_id': {
                        'location': 'location_a',
                        'contour': 'contour_a',
                        'stand': 'stand_a'},
                    'revision': 'HEAD'},
                'config_info_b': {
                    'config_id': {
                        'location': 'location_b',
                        'contour': 'contour_b',
                        'stand': 'stand_b'},
                    'revision': 'HEAD'},
                'diff_result': []}],
        "topology_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        },
        "dns_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        }
    })
    assert len(session_diff.config_diff.list) == 1


def test_session_diff_to_json():
    config_id_a = ConfigurationId("contour_a", "stand_a", "location_a")
    config_info_a = ConfigurationInfo(config_id_a, "HEAD")
    config_id_b = ConfigurationId("contour_b", "stand_b", "location_b")
    config_info_b = ConfigurationInfo(config_id_b, "HEAD")
    diff_result = DiffResult(config_info_a, config_info_b, GeneratorResultList())
    diff_results = ConfigResultList([diff_result])
    session_diff = SessionDiff(diff_results, TopologyDiff([], [], {}), DnsDiff([], [], {}))
    assert session_diff.to_json() == {
        "config_diff":
            [{
                'config_info_a': {
                    'config_id': {
                        'location': 'location_a',
                        'contour': 'contour_a',
                        'stand': 'stand_a'},
                    'revision': 'HEAD'},
                'config_info_b': {
                    'config_id': {
                        'location': 'location_b',
                        'contour': 'contour_b',
                        'stand': 'stand_b'},
                    'revision': 'HEAD'},
                'diff_result': []}],
        "topology_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        },
        "dns_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        }
    }


def test_session_has_diff_no():
    session_diff = SessionDiff.from_json({
        "config_diff":
            [{
                'config_info_a': {
                    'config_id': {
                        'location': 'location_a',
                        'contour': 'contour_a',
                        'stand': 'stand_a'},
                    'revision': 'HEAD'},
                'config_info_b': {
                    'config_id': {
                        'location': 'location_b',
                        'contour': 'contour_b',
                        'stand': 'stand_b'},
                    'revision': 'HEAD'},
                'diff_result': []}],
        "topology_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        },
        "dns_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        }
    }
    )
    assert not session_diff.has_diff()


def test_session_has_diff_no_data():
    session_diff = SessionDiff.from_json({
        "config_diff":
            [{
                'config_info_a': {
                    'config_id': {
                        'location': 'location_a',
                        'contour': 'contour_a',
                        'stand': 'stand_a'},
                    'revision': 'HEAD'},
                'config_info_b': {
                    'config_id': {
                        'location': 'location_b',
                        'contour': 'contour_b',
                        'stand': 'stand_b'},
                    'revision': 'HEAD'},
                'diff_result': [
                    {
                        'generator': 'custom',
                        'files': [
                            {'content': '', 'name': 'simple.diff'},
                            {'content': '', 'name': 'another.diff'}
                        ],
                        'results': []
                    }]}],
        "topology_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        },
        "dns_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        }
    }
    )
    assert not session_diff.has_diff()


def test_session_has_diff_yes_simple():
    session_diff = SessionDiff.from_json({
        "config_diff":
            [{
                'config_info_a': {
                    'config_id': {
                        'location': 'location_a',
                        'contour': 'contour_a',
                        'stand': 'stand_a'},
                    'revision': 'HEAD'},
                'config_info_b': {
                    'config_id': {
                        'location': 'location_b',
                        'contour': 'contour_b',
                        'stand': 'stand_b'},
                    'revision': 'HEAD'},
                'diff_result': [
                    {
                        'generator': 'custom',
                        'files': [
                            {'content': '+diff', 'name': 'simple.diff'},
                            {'content': '', 'name': 'another.diff'}
                        ],
                        'results': []
                    }]}],
        "topology_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        },
        "dns_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        }
    })
    assert session_diff.has_diff()


def test_session_has_diff_yes_generators():
    session_diff = SessionDiff.from_json({
        "config_diff":
            [{
                'config_info_a': {
                    'config_id': {
                        'location': 'location_a',
                        'contour': 'contour_a',
                        'stand': 'stand_a'},
                    'revision': 'HEAD'},
                'config_info_b': {
                    'config_id': {
                        'location': 'location_b',
                        'contour': 'contour_b',
                        'stand': 'stand_b'},
                    'revision': 'HEAD'},
                'diff_result': [
                    {
                        'generator': 'custom',
                        'files': [
                            {'content': '+diff', 'name': 'simple.diff'},
                            {'content': '', 'name': 'another.diff'}
                        ],
                        'results': []
                    },
                    {
                        'generator': 'custom2',
                        'files': [
                            {'content': '', 'name': 'another.diff'}
                        ],
                        'results': []
                    }
                ]}],
        "topology_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        },
        "dns_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        }
    })
    assert session_diff.has_diff()


def test_session_has_diff_yes_topology():
    session_diff = SessionDiff.from_json({
        "config_diff":
            [{
                'config_info_a': {
                    'config_id': {
                        'location': 'location_a',
                        'contour': 'contour_a',
                        'stand': 'stand_a'},
                    'revision': 'HEAD'},
                'config_info_b': {
                    'config_id': {
                        'location': 'location_b',
                        'contour': 'contour_b',
                        'stand': 'stand_b'},
                    'revision': 'HEAD'},
                'diff_result': []
            }],
        "topology_diff": {
            "added": ["<some group>"],
            "removed": [],
            "modified": {},
        },
        "dns_diff": {
            "added": [],
            "removed": [],
            "modified": {},
        }
    })
    assert session_diff.has_diff()
