# -*- coding: utf-8 -*-


from itertools import count
from typing import Dict, Iterable, List, Optional, Set, Type, Union
import dataclasses
# noinspection PyUnresolvedReferences
import pytest

from travel.hotels.content_manager.config.stage_config import StageConfig
from travel.hotels.content_manager.data_model.options import (
    NirvanaWorkflowOptions, StageOptions, TolokaPoolOptions, TriggerOptions
)
from travel.hotels.content_manager.data_model.storage import (
    StorageHotelWL, StorageMapping, StoragePermalink, StoragePermalinkWL, StoragePermaroom, StorageSCDescription
)
from travel.hotels.content_manager.data_model.storage import StorageUrl
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict, get_dc_yt_schema
from travel.hotels.content_manager.lib.delayed_executor import DelayedExecutor
from travel.hotels.content_manager.lib.path_info import PathInfo
from travel.hotels.content_manager.lib.path_mapping import PathMapping
from travel.hotels.content_manager.lib.persistence_manager import LocalPersistenceManager
from travel.hotels.content_manager.lib.storage import Storage
from travel.hotels.content_manager.lib.trigger import Trigger
from travel.hotels.content_manager.lib.updater import Updater


class DefaultDict(dict):

    def __init__(self, default_value, **kwargs):
        super().__init__(**kwargs)
        self.default_value = default_value

    def __getattr__(self, item):
        return self.setdefault(item, self.default_value)

    def get(self, item, default=None):
        return self.setdefault(item, default or self.default_value)


class Helper(object):

    def __init__(self):
        self.yt_root = '//home/test_root'
        self.persistence_manager = LocalPersistenceManager(self.yt_root, './test_root')
        path_info = PathInfo(self.persistence_manager, self.yt_root)
        self.path_info = path_info
        self.entity_to_path = PathMapping(path_info).entity_to_path

    def prepare_storage(
        self,
        hotels_wl=None,
        permalinks=None,
        permalinks_wl=None,
        permarooms=None,
        mappings=None,
        urls=None,
        sc_descriptions=None,
    ):
        if hotels_wl is None:
            hotels_wl = list()
        if permalinks is None:
            permalinks = list()
        if permalinks_wl is None:
            permalinks_wl = list()
        if permarooms is None:
            permarooms = list()
        if mappings is None:
            mappings = list()
        if urls is None:
            urls = list()
        if sc_descriptions is None:
            sc_descriptions = list()
        hotels_wl = (dc_to_dict(p) for p in hotels_wl)
        permalinks = (dc_to_dict(p) for p in permalinks)
        permalinks_wl = (dc_to_dict(p) for p in permalinks_wl)
        permarooms = (dc_to_dict(p) for p in permarooms)
        mappings = (dc_to_dict(m) for m in mappings)
        urls = (dc_to_dict(u) for u in urls)
        sc_descriptions = (dc_to_dict(d) for d in sc_descriptions)
        persistence_manager = self.persistence_manager
        path_info = self.path_info

        if persistence_manager.exists(path_info.storage_path):
            persistence_manager.delete(path_info.storage_path)

        persistence_manager.write(path_info.storage_hotels_wl_table, hotels_wl, get_dc_yt_schema(StorageHotelWL))
        persistence_manager.write(path_info.storage_permalinks_table, permalinks, get_dc_yt_schema(StoragePermalink))
        persistence_manager.write(
            path_info.storage_permalinks_wl_table, permalinks_wl, get_dc_yt_schema(StoragePermalinkWL)
        )
        persistence_manager.write(path_info.storage_permarooms_table, permarooms, get_dc_yt_schema(StoragePermaroom))
        persistence_manager.write(path_info.storage_mappings_table, mappings, get_dc_yt_schema(StorageMapping))
        persistence_manager.write(path_info.storage_urls_table, urls, get_dc_yt_schema(StorageUrl))
        persistence_manager.write(
            path_info.storage_sc_descriptions_table, sc_descriptions, get_dc_yt_schema(StorageSCDescription)
        )

    def get_storage(self) -> Storage:
        table_data = dict()
        for entity_cls, table_path in self.entity_to_path.items():
            table_data[entity_cls] = list(self.persistence_manager.read(table_path))
        storage = Storage()
        storage.apply_data(table_data)
        return storage

    def get_trigger_path_producer(self, dir_names: Iterable[str]):
        dir_names = count()

        # noinspection PyShadowingNames
        def get_new_trigger_path(self):
            dir_name = next(dir_names)
            return self.persistence_manager.join(self.stage_input_path, str(dir_name))

        return get_new_trigger_path

    def get_trigger(
            self,
            config: StageConfig,
            start_ts: int = 0,
            options: StageOptions = None,
    ) -> Trigger:
        delayed_executor = DelayedExecutor()

        if options is None:
            filter_names = ['default']
        else:
            filter_names = options.triggers.keys()

        Trigger.get_new_trigger_path = self.get_trigger_path_producer(filter_names)

        trigger = Trigger(
            process_type=config.process_type,
            stage_name=config.name,
            producer_cls=config.producer,
            thread_filters=config.filters,
            persistence_manager=self.persistence_manager,
            delayed_executor=delayed_executor,
            path_info=self.path_info,
            entity_cls=config.entity_cls,
            other_entities=config.other_entities,
            start_ts=start_ts,
            options=options,
            jobs_max=config.jobs_max,
            job_size=config.job_size,
            manual_start=config.manual_start,
            run_on_storage_change=config.run_on_storage_change,
        )
        self.persistence_manager.delete(trigger.stage_input_path)
        return trigger

    def get_updater(
            self, config: Type[StageConfig],
            start_ts: int = 0,
            options: Optional[Dict[str, StageOptions]] = None,
    ) -> Updater:
        if options is None:
            options = dict()

        # noinspection PyCallByClass
        updater = config.updater(
            stage_name=config.name,
            persistence_manager=self.persistence_manager,
            yql_client=None,
            path_info=self.path_info,
            start_ts=start_ts,
            save_history=config.save_history,
            options=options,
        )
        return updater

    def get_updater_args(self, name):
        stages_path = self.path_info.stages_path
        output_path = self.persistence_manager.join(stages_path, name, 'fake_uid', 'output')
        temp_dir = self.persistence_manager.join(self.path_info.temp_path, 'fake_uid')
        return output_path, temp_dir


class Checker:

    def _compare_permalinks(
        self,
        entity_cls: Type[dataclasses.dataclass],
        expected_permalinks: List[StoragePermalinkWL],
        storage: Storage,
        fields_to_ignore: Set[str],
    ):
        expected_permalinks = {p.permalink: p for p in expected_permalinks}
        expected_permalink_ids = set(expected_permalinks.keys())
        actual_permalinks = storage.permalinks_wl
        actual_permalink_ids = set(actual_permalinks.keys())
        extra_permalink_ids = expected_permalink_ids - actual_permalink_ids
        if extra_permalink_ids:
            extra_permalinks = '\n'.join(str(expected_permalinks[p_id]) for p_id in extra_permalink_ids)
            raise Exception(f'More expected than actual: \n{extra_permalinks}')
        extra_permalink_ids = actual_permalink_ids - expected_permalink_ids
        if extra_permalink_ids:
            extra_permalinks = '\n'.join(str(actual_permalinks[p_id]) for p_id in extra_permalink_ids)
            raise Exception(f'More actual then expected: \n{extra_permalinks}')
        for permalink_id, expected_permalink in expected_permalinks.items():
            actual_permalink = actual_permalinks[permalink_id]
            self._compare_dc(entity_cls, permalink_id, expected_permalink, actual_permalink, fields_to_ignore)

    @staticmethod
    def _compare_dc(
        entity_cls: Type[dataclasses.dataclass],
        entity_id: str,
        expected_dc: dataclasses.dataclass,
        actual_dc: dataclasses.dataclass,
        fields_to_ignore: Set[str],
    ):
        for field in dataclasses.fields(entity_cls):
            if field.name in fields_to_ignore:
                continue
            expected_value = getattr(expected_dc, field.name)
            actual_value = getattr(actual_dc, field.name)
            if expected_value != actual_value:
                raise Exception(f'{entity_id=}, {field.name} expected {expected_value} but got {actual_value}')


class TriggerChecker(Checker):

    def __init__(
        self,
        helper: Helper,
        stage_config: Type[StageConfig],
        entity_cls: Type[dataclasses.dataclass],
        input_data_cls: Type[dataclasses.dataclass],
        input_table_name: str,
        fields_to_ignore: Optional[List[str]] = None,
        start_ts: int = 0,
    ):
        self.helper = helper
        self.entity_cls = entity_cls
        self.input_data_cls = input_data_cls
        self.input_table_name = input_table_name
        if fields_to_ignore is None:
            fields_to_ignore = list()
        self.fields_to_ignore = set(fields_to_ignore)

        default_options = TriggerOptions(
            workflow_options=NirvanaWorkflowOptions(
                singleProjectId='0',
                singlePoolId='0',
            ),
            pool_options=TolokaPoolOptions(
                reward_per_assignment=0,
            ),
        )
        common_basic_options = TriggerOptions(
            workflow_options=NirvanaWorkflowOptions(
                singleProjectId='1',
                singlePoolId='1',
            ),
            pool_options=TolokaPoolOptions(
                reward_per_assignment=1,
            ),
        )
        common_advanced_options = TriggerOptions(
            workflow_options=NirvanaWorkflowOptions(
                singleProjectId='2',
                singlePoolId='2',
            ),
            pool_options=TolokaPoolOptions(
                reward_per_assignment=1,
            ),
        )
        common_edit_options = TriggerOptions(
            workflow_options=NirvanaWorkflowOptions(
                singleProjectId='3',
                singlePoolId='3',
            ),
            pool_options=TolokaPoolOptions(
                reward_per_assignment=2,
            ),
        )
        trigger_options = DefaultDict(
            default_options,
            common_basic=common_basic_options,
            common_advanced=common_advanced_options,
            common_edit=common_edit_options,
        )
        options = StageOptions(triggers=trigger_options)
        # noinspection PyDataclass
        stage_config = dataclasses.replace(stage_config, jobs_max=3)
        self.trigger = helper.get_trigger(stage_config, start_ts=start_ts, options=options)

    def check_permalink_wl(
        self,
        input_permalink: StoragePermalinkWL,
        expected_permalink: StoragePermalinkWL,
        expected_input: dataclasses.dataclass,
    ) -> None:
        if not isinstance(input_permalink, list):
            input_permalink = [input_permalink]
        if not isinstance(expected_permalink, list):
            expected_permalink = [expected_permalink]

        self.helper.prepare_storage(permalinks_wl=input_permalink)
        self.trigger.process()

        # storage
        storage = self.helper.get_storage()

        self._compare_permalinks(self.entity_cls, expected_permalink, storage, self.fields_to_ignore)

        trigger_data_path = self.helper.persistence_manager.join(
            self.trigger.stage_input_path, '0', self.input_table_name
        )
        data = self.helper.persistence_manager.read(trigger_data_path)
        data = list(data)
        assert 1 == len(data)
        actual_input = dc_from_dict(self.input_data_cls, data[0]['input'])

        try:
            self._compare_dc(
                self.input_data_cls, self.input_table_name, expected_input, actual_input, self.fields_to_ignore
            )
        finally:
            if self.helper.persistence_manager.exists(self.trigger.stage_input_path):
                self.helper.persistence_manager.delete(self.trigger.stage_input_path)


class UpdaterChecker(Checker):

    def __init__(
        self,
        helper: Helper,
        stage_config: Type[StageConfig],
        entity_cls: Type[dataclasses.dataclass],
        processor_result_cls: Type[dataclasses.dataclass],
        output_table_name: str,
        options: Optional[Dict[str, StageOptions]] = None,
        fields_to_ignore: Optional[List[str]] = None,
        start_ts: int = 0,
    ):
        self.helper = helper
        self.entity_cls = entity_cls
        self.processor_result_cls = processor_result_cls
        if fields_to_ignore is None:
            fields_to_ignore = list()
        self.fields_to_ignore = set(fields_to_ignore)

        self.updater = self.helper.get_updater(stage_config, start_ts, options)
        self.updater_args = self.helper.get_updater_args(stage_config.name)

        output_path, _ = self.updater_args
        self.processor_result_path = self.helper.persistence_manager.join(output_path, output_table_name)

    def check_permalink_wl(
        self,
        input_permalinks: Union[StoragePermalinkWL, List[StoragePermalinkWL]],
        processor_result: dataclasses.dataclass,
        expected_permalinks: Union[StoragePermalinkWL, List[StoragePermalinkWL]],
    ) -> None:
        if not isinstance(input_permalinks, list):
            input_permalinks = [input_permalinks]
        if not isinstance(expected_permalinks, list):
            expected_permalinks = [expected_permalinks]

        self.helper.prepare_storage(permalinks_wl=input_permalinks)
        processor_result = [dc_to_dict(processor_result)]
        self.helper.persistence_manager.write(
            self.processor_result_path, processor_result, get_dc_yt_schema(self.processor_result_cls)
        )

        self.updater.run(*self.updater_args)

        # storage
        storage = self.helper.get_storage()

        self._compare_permalinks(self.entity_cls, expected_permalinks, storage, self.fields_to_ignore)


@pytest.fixture
def helper():
    return Helper()
