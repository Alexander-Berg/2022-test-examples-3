import logging
from typing import List

import click
import pytest
from click.testing import CliRunner

from market.monetize.stapler.v1.tasks.arg import Arg
from market.monetize.stapler.v1.tasks.chyt import ChytTask
from market.monetize.stapler.v1.tasks.cli import TasksCli
from market.monetize.stapler.v1.tasks.yql import YqlTask


class SomeYqlTask(YqlTask):
    def get_task_args(self) -> List[Arg]:
        return []

    def run(self):
        logging.info(f'{self.__class__.__name__} run')

        query_text = 'select 1;'
        query = self.query(query_text)
        query.run()

        click.echo(f'{True}')


class SomeChytTask(ChytTask):
    def get_task_args(self) -> List[Arg]:
        return []

    def run(self):
        logging.info(f'{self.__class__.__name__} run')

        result = self.query('select 1 as value;')

        result = list(result)
        assert len(result)
        assert result[0]['value'] == 1

        click.echo(f'{True}')


some_yql_task = SomeYqlTask()
some_chyt_task = SomeChytTask()
test_cli = TasksCli(
    some_yql_task,
    some_chyt_task,
)


@test_cli.build_cli()
def cli():
    pass


runner = CliRunner()


@pytest.mark.usefixtures('yql_mock', 'chyt_mock')
class TestTasks:

    @pytest.mark.parametrize('cls', [
        some_yql_task,
        some_chyt_task,
    ])
    def test_cli(self, cls):
        assert cli
        assert cli.commands.get(cls.__class__.__name__)

    def test_yql(self):
        command = cli.commands.get(some_yql_task.__class__.__name__)

        result = runner.invoke(cli, [command.name])

        logging.info(result)

        assert result
        assert result.exit_code == 0
        assert result.output == f'{True}\n'

    def test_chyt(self):
        command = cli.commands.get(some_chyt_task.__class__.__name__)

        result = runner.invoke(cli, [command.name])

        logging.info(result)

        assert result
        assert result.exit_code == 0
        assert result.output == f'{True}\n'
