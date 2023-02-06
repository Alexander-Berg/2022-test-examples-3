from market.idx.marketindexer.marketindexer.in_out_checker import input_output, input, output, Check, MiConfigFlags, \
    InputFileNotFoundException, OutputFileNotFoundException, MasterGenerationDirInput, WorkerWorkindexDir, DumpersOn
import tempfile
import os
import shutil
import pytest
import mock
import market.idx.pylibrary.mindexer_core.market_collections.market_collections as market_collections

GENERATION_NAME = '19841017'


def touch(filepath):
    basedir = os.path.dirname(filepath)
    if not os.path.exists(basedir):
        os.makedirs(basedir)
    open(filepath, 'w')


COLLECTION_JSON_CONTENT = """
{
  "offers": [
    {
      "xmlconfig": "config-0.xml",
      "offersconfig": "offers-0.conf",
      "nparts": 16,
      "host": "idx-duty-forever.market.yandex.net",
      "parts": [
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15
      ],
      "dists": [
        "search-part-0",
        "search-part-1",
        "search-part-2",
        "search-part-3",
        "search-part-4",
        "search-part-5",
        "search-part-6",
        "search-part-7",
        "search-part-8",
        "search-part-9",
        "search-part-10",
        "search-part-11",
        "search-part-12",
        "search-part-13",
        "search-part-14",
        "search-part-15"
      ],
      "id": 0
    }
  ]
}
"""


class MiConfig(object):
    def __init__(self,
                 working_dir,
                 feature_0_flag=False,
                 feature_1_flag_0=False,
                 feature_1_flag_1=False,
                 enable_in_out_checks=True,
                 dumpers='',
                 optional_dumpers='',
                 in_out_checks_skip_steps_list=None,
                 ):
        if in_out_checks_skip_steps_list is None:
            in_out_checks_skip_steps_list = []
        self.feature_0_flag = feature_0_flag
        self.feature_1_flag_0 = feature_1_flag_0
        self.feature_1_flag_1 = feature_1_flag_1
        self.enable_in_out_checks = enable_in_out_checks
        self.working_dir = working_dir
        self.genlog_dumpers_types = dumpers
        self.genlog_optional_dumpers_types = optional_dumpers
        self.in_out_checks_skip_steps_list = in_out_checks_skip_steps_list


class BuildMassIndex(object):
    def __init__(self, config, generation_name):
        self.config = config
        self.generation_name = generation_name
        self.collection_json_path = os.path.join(self.config.working_dir, 'offers', self.generation_name, 'config', 'collection.json')
        self._write_collectoin_json()

    def _write_collectoin_json(self):
        dirname = os.path.dirname(self.collection_json_path)
        if not os.path.exists(dirname):
            os.makedirs(dirname)
        with open(self.collection_json_path, 'w+') as f:
            f.write(COLLECTION_JSON_CONTENT)
        return self

    @property
    def offers_indexer(self):
        return market_collections.OffersCollection(self.config.working_dir, self.generation_name)

    def _create_file_in_generation_input(self, file_name):
        touch(os.path.join(self.config.working_dir, self.generation_name, 'input', file_name))

    @output(
        Check().condition(MiConfigFlags('feature_0_flag'))
               .file(MasterGenerationDirInput('feature_0_master_out_0', 'feature_0_master_out_1')),
        Check().file(MasterGenerationDirInput('unconditional_output'))
    )
    def func_correct_output(self):
        # Создаём все выходные файлы
        if self.config.feature_0_flag:
            self._create_file_in_generation_input('feature_0_master_out_0')
            self._create_file_in_generation_input('feature_0_master_out_1')
        self._create_file_in_generation_input('unconditional_output')

    @input_output(
        output_checks=[
            Check().file(MasterGenerationDirInput('out_file'))
        ]
    )
    def func_incorrect_output(self):
        # Не создаём задекларированный выходной файл out_file
        pass

    @input(
        Check().file(MasterGenerationDirInput('absent_file_666'))
    )
    def func_miss_input(self):
        pass

    @output(
        Check().condition(MiConfigFlags('feature_1_flag_0', 'feature_1_flag_1'))
               .file(MasterGenerationDirInput('feature_0_master_out_1'))
    )
    def feature_1_func(self):
        if self.config.feature_1_flag_0 and self.config.feature_1_flag_1:
            self._create_file_in_generation_input('feature_0_master_out_1')

    @output(
        Check().condition(MiConfigFlags('feature_1_flag_0', 'feature_1_flag_1'))
               .file(MasterGenerationDirInput('feature_1_out_file'))
    )
    def feature_1_func_incorrect(self):
        pass

    @input(
        Check().file(WorkerWorkindexDir('worker_in_file_0', 'worker_in_file_1'))
    )
    def worker_feature_0(self):
        pass

    @output(
        Check().condition(DumpersOn('dumper0', 'dumper1')).file(MasterGenerationDirInput('dumper_output'))
    )
    def feature_dumper_correct(self):
        dumpers = set(self.config.genlog_dumpers_types.split(' ')).union(set(self.config.genlog_optional_dumpers_types.split(' ')))
        if 'dumper0' in dumpers and 'dumper1' in dumpers:
            self._create_file_in_generation_input('dumper_output')

    @output(
        Check().condition(DumpersOn('dumper0', 'dumper1')).file(MasterGenerationDirInput('dumper_output'))
    )
    def feature_dumper_incorrect(self):
        pass


# Проверить, что имя функции не меняется декоратором
def test_func_name():
    def name(f):
        return f.func_name

    class BMI(object):
        @input(
            Check().file(MasterGenerationDirInput('some_file'))
        )
        def some_func_0(self):
            pass

        @output(
            Check().file(MasterGenerationDirInput('some_file'))
        )
        def some_func_1(self):
            pass

        @input_output(
            input_checks=[Check().file(MasterGenerationDirInput('some_file'))],
            output_checks=[Check().file(MasterGenerationDirInput('some_other_file'))]
        )
        def some_func_2(self):
            pass

        def assert_names(self):
            assert(name(self.some_func_0) == 'some_func_0')
            assert(name(self.some_func_1) == 'some_func_1')
            assert(name(self.some_func_2) == 'some_func_2')

    b = BMI()
    b.assert_names()


# Проверка на наличие выходного файла - успешная.
def test_output_localfile_ok():
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, feature_0_flag=True, enable_in_out_checks=True)

    try:
        touch(os.path.join(config.working_dir, GENERATION_NAME, 'in_file'))

        b = BuildMassIndex(config, GENERATION_NAME)
        b.func_correct_output()
    finally:
        shutil.rmtree(tmp_dir)


# Файла входного нет, но проверка выключена - проверяем, что всё ок.
def test_skip_check():
    tmp_dir = tempfile.mkdtemp()
    try:
        config = MiConfig(tmp_dir, feature_0_flag=True, enable_in_out_checks=True, in_out_checks_skip_steps_list=['func_correct_output'])

        b = BuildMassIndex(config, GENERATION_NAME)
        b.func_correct_output()
    finally:
        shutil.rmtree(tmp_dir)


# Проверка на наличие выходного файла - не успешная.
def test_output_localfile_exception():
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, enable_in_out_checks=True)

    with pytest.raises(OutputFileNotFoundException):
        try:
            b = BuildMassIndex(config, GENERATION_NAME)
            b.func_incorrect_output()
        finally:
            shutil.rmtree(tmp_dir)


# Проверка на наличие входног офайла - не успешная
def test_input_localfile_exception():
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, enable_in_out_checks=True)

    with pytest.raises(InputFileNotFoundException):
        try:
            touch(os.path.join(config.working_dir, GENERATION_NAME, 'in_file'))

            b = BuildMassIndex(config, GENERATION_NAME)
            b.func_miss_input()
        finally:
            shutil.rmtree(tmp_dir)


# Проверка входных/выходных данных выключена. Функции на которых проверка была
# не успешна теперь молча отрабатывают.
def test_check_disabled_off_no_checks():
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, feature_0_flag=True, enable_in_out_checks=False)

    try:
        b = BuildMassIndex(config, GENERATION_NAME)
        b.func_miss_input()
        b.func_incorrect_output()
    finally:
        shutil.rmtree(tmp_dir)


# Выключен фича флаг. Проверка, задействующая этот флаг должна отключиться.
# Функция func_correct_output не должна кинуть исключение
def test_condition_off_no_check():
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, feature_0_flag=False, enable_in_out_checks=True)

    try:
        b = BuildMassIndex(config, GENERATION_NAME)
        b.func_correct_output()
    finally:
        shutil.rmtree(tmp_dir)


# В feature_1_func() используется 2 флага. Функция ведёт себя корректно, поэтому
# без исключений.
@pytest.mark.parametrize('flag0, flag1', [(False, False), (False, True), (True, False), (True, True)])
def test_multiple_condition_check_ok(flag0, flag1):
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir,
                      feature_1_flag_0=flag0,
                      feature_1_flag_1=flag1,
                      enable_in_out_checks=True)

    try:
        b = BuildMassIndex(config, GENERATION_NAME)
        b.feature_1_func()
    finally:
        shutil.rmtree(tmp_dir)


# В feature_1_func_incorrect() используется 2 флага. Функция ведёт себя не корректно -
# не создаёт файл. Ожидаем исключение в случае включённых обоих флагов
@pytest.mark.parametrize('flag0, flag1, throw_exception_expected', [(False, False, False),
                                                                    (False, True, False),
                                                                    (True, False, False),
                                                                    (True, True, True)])
def test_multiple_condition_check_exception(flag0, flag1, throw_exception_expected):
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir,
                      feature_1_flag_0=flag0,
                      feature_1_flag_1=flag1,
                      enable_in_out_checks=True)

    def test_core():
        try:
            b = BuildMassIndex(config, GENERATION_NAME)
            b.feature_1_func_incorrect()
        finally:
            shutil.rmtree(tmp_dir)

    if throw_exception_expected:
        with pytest.raises(OutputFileNotFoundException):
            test_core()
    else:
        test_core()


def test_input_file_on_worker_ok():
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, enable_in_out_checks=True)

    try:
        def start_and_wait_tasks(tasks, callback=None, error_callback=None, timeout=None):
            assert(len(tasks) == 1)
            task = tasks.keys()[0]
            assert(task.host == 'idx-duty-forever.market.yandex.net')
            assert(task.port == 29320)  # default port
            assert(task.name == 'oi.check_files_in_workindex')
            assert(len(task.args) == 3)
            relative_file_paths = task.args[0]
            worker_generation = task.args[1]
            shards = task.args[2]
            assert(set(relative_file_paths) == set(['worker_in_file_0', 'worker_in_file_1']))
            assert(worker_generation == '{}-0'.format(GENERATION_NAME))
            assert(shards == [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15])

            # Говорим, что все файлы есть.
            return {}

        @mock.patch('market.idx.pylibrary.mindexer_core.mifrpc.mifrpc.start_and_wait_tasks', start_and_wait_tasks)
        def run_test():
            b = BuildMassIndex(config, GENERATION_NAME)
            b.worker_feature_0()

        run_test()

    finally:
        shutil.rmtree(tmp_dir)


@pytest.mark.parametrize('dumpers, optional_dumpers', [('PROMO DIMENSIONS', 'QQQ'),
                                                       ('dumper0', 'QQQ'),
                                                       ('QQQ', 'dumper1'),
                                                       ('', 'dumper0 QQQ dumper1'),
                                                       ('dumper0 QQQ dumper1', ''),
                                                       ('dumper0 QQQ dumper1', 'dumper1')])
def test_dumpers_on_ok(dumpers, optional_dumpers):
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, enable_in_out_checks=True, dumpers=dumpers, optional_dumpers=optional_dumpers)

    try:
        b = BuildMassIndex(config, GENERATION_NAME)
        b.feature_dumper_correct()

    finally:
        shutil.rmtree(tmp_dir)


@pytest.mark.parametrize('dumpers, optional_dumpers', [('PROMO DIMENSIONS', 'QQQ'),
                                                       ('dumper0', 'QQQ'),
                                                       ('QQQ', 'dumper1')])
def test_dumpers_on_incorrect_ok(dumpers, optional_dumpers):
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, enable_in_out_checks=True, dumpers=dumpers, optional_dumpers=optional_dumpers)

    try:
        b = BuildMassIndex(config, GENERATION_NAME)
        b.feature_dumper_incorrect()

    finally:
        shutil.rmtree(tmp_dir)


@pytest.mark.parametrize('dumpers, optional_dumpers', [('dumper0 dumper1', 'QQQ'),
                                                       ('dumper0', 'dumper1'),
                                                       ('QQQ', 'dumper1 dumper0')])
def test_dumpers_on_incorrect_exception(dumpers, optional_dumpers):
    tmp_dir = tempfile.mkdtemp()
    config = MiConfig(tmp_dir, enable_in_out_checks=True, dumpers=dumpers, optional_dumpers=optional_dumpers)

    with pytest.raises(OutputFileNotFoundException):
        try:
            b = BuildMassIndex(config, GENERATION_NAME)
            b.feature_dumper_incorrect()

        finally:
            shutil.rmtree(tmp_dir)
