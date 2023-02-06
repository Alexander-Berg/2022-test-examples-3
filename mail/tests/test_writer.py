import asyncio
import gzip

import pytest

from sendr_writers.base.writer import Writer


@pytest.fixture
def base_log_path(tmp_path):
    return tmp_path / 'dummy.log'


class TestUncompressedWriter:
    @pytest.fixture
    async def writer(self, base_log_path, loop):
        writer = Writer(
            base_file_name=str(base_log_path),
            rotate_interval=1,
            backup_count=1,
            loop=loop,
        )
        writer.run()
        yield writer
        await writer.close()

    @pytest.mark.asyncio
    async def test_write(self, tmp_path, base_log_path, writer):
        await writer.write(b'test')
        await writer._close_task()

        content = {f.name: f.read_text().strip() for f in tmp_path.iterdir()}
        assert content == {base_log_path.name: 'test'}

    @pytest.mark.asyncio
    async def test_rotate(self, tmp_path, base_log_path, writer):
        await writer.write(b'first')
        await asyncio.sleep(1)
        await writer.write(b'second')
        await writer._close_task()

        content = {f.name: f.read_text().strip() for f in tmp_path.iterdir()}
        assert content == {
            base_log_path.name: 'second',
            f'{base_log_path.name}.1': 'first',
        }


class TestCompressedWriter:
    @pytest.fixture
    def compressed_log_filename(self, base_log_path):
        return f'{base_log_path.name}.gz'

    @pytest.fixture
    async def writer(self, base_log_path, loop):
        writer = Writer(
            base_file_name=str(base_log_path),
            rotate_interval=1,
            backup_count=2,
            loop=loop,
            compress_on_backup=True,
        )
        writer.run()
        yield writer
        await writer.close()

    @pytest.mark.asyncio
    async def test_write(self, tmp_path, base_log_path, writer):
        await writer.write(b'test')
        await writer._close_task()

        content = {
            f.name: f
            for f in tmp_path.iterdir()
        }
        assert content.keys() == {base_log_path.name}
        assert content[base_log_path.name].read_text().strip() == 'test'

    @pytest.mark.asyncio
    async def test_rotate(self, tmp_path, base_log_path, compressed_log_filename, writer):
        await writer.write(b'first')
        await asyncio.sleep(1)
        await writer.write(b'second')
        await asyncio.sleep(1)
        await writer.write(b'third')
        await writer._close_task()

        content = {f.name: f for f in tmp_path.iterdir()}
        assert content.keys() == {
            base_log_path.name,
            f'{compressed_log_filename}.1',
            f'{compressed_log_filename}.2',
        }
        assert content[base_log_path.name].read_text().strip() == 'third'

        for num, value in enumerate(['second', 'first'], 1):
            with gzip.open(content[f'{compressed_log_filename}.{num}']) as f:
                assert f.read().decode().strip() == value
