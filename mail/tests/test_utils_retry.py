import pytest

from sendr_utils.retry import RetryLimitExceededError, async_retry


class DummyException(Exception):
    pass


class TestRetry:
    @pytest.fixture
    def args(self):
        return (1, 2)

    @pytest.fixture
    def kwargs(self):
        return {'a': 3}

    @pytest.fixture
    def delay(self):
        return 0.1

    @pytest.fixture
    def retries(self):
        return 3

    @pytest.fixture
    def retrying(self, retryable, delay, retries):
        decorator = async_retry(base_delay=delay,
                                retries=retries,
                                exceptions=(DummyException,),
                                )
        return decorator(retryable)

    @pytest.fixture
    def expected_call_count_on_error(self, retries):
        return retries + 1

    @pytest.fixture(autouse=True)
    def mock_sleep(self, mocker, coromock, retries):
        return mocker.patch('sendr_utils.retry.sleep', coromock())

    class TestSuccess:
        @pytest.fixture
        def expected(self):
            return '123'

        @pytest.fixture
        def retryable(self, mocker, coromock, expected):
            return coromock(expected)

        @pytest.fixture(autouse=True)
        async def returned(self, loop, retrying, args, kwargs):
            return await retrying(*args, **kwargs)

        def test_success_called_once(self, retryable):
            assert retryable.call_count == 1

        def test_success_args(self, args, kwargs, retryable):
            assert retryable.call_args_list == [(args, kwargs)]

        def test_success_returned(self, returned, expected):
            assert returned == expected

    class TestException:
        @pytest.fixture
        def retryable(self, mocker):
            return mocker.Mock(side_effect=DummyException)

        @pytest.mark.asyncio
        async def test_exception_raises(self, retrying, args, kwargs):
            with pytest.raises(RetryLimitExceededError) as exc_info:
                await retrying(*args, **kwargs)

            assert isinstance(exc_info.value.last_exception, DummyException)

        class TestCallsWhenException:
            @pytest.fixture(autouse=True)
            async def run(self, loop, retrying, args, kwargs):
                with pytest.raises(RetryLimitExceededError):
                    return await retrying(*args, **kwargs)

            def test_exception_retried_n_times(self, retryable, expected_call_count_on_error):
                assert retryable.call_count == expected_call_count_on_error

            def test_exception_args(self, args, kwargs, retryable, expected_call_count_on_error):
                assert retryable.call_args_list == [(args, kwargs)] * expected_call_count_on_error

            @pytest.mark.asyncio
            async def test_exception_calls_sleep(self, mock_sleep, retries):
                assert mock_sleep.call_count == retries

        class TestThrowLastException:
            @pytest.fixture
            def retrying(self, retryable, delay, retries):
                decorator = async_retry(base_delay=delay,
                                        retries=retries,
                                        exceptions=(DummyException,),
                                        wrap_exception=False,
                                        )
                return decorator(retryable)

            @pytest.mark.asyncio
            async def test_throw_last_exception_raises(self, retrying, args, kwargs):
                with pytest.raises(DummyException):
                    return await retrying(*args, **kwargs)
