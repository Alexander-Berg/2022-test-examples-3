import contextlib
import socket
import subprocess
import time


# Starts a userver based service and retuns when it is ready to accept requests
@contextlib.contextmanager
def start_uservice(
        *,
        service_path: str,
        static_config: str,
        port: int,
        timeout: float,
        host: str = 'localhost'
        ) -> None:
    service = subprocess.Popen([service_path, '--config', static_config])

    start_time = time.perf_counter()
    while True:
        passed_time = time.perf_counter() - start_time
        if passed_time >= timeout:
            service.terminate()
            raise TimeoutError(
                f'Waited too long for the {service_path} '
                f'on host {host} to start accepting connections.'
            )

        try:
            with socket.create_connection(
                    (host, port), timeout=timeout-passed_time,
            ):
                break
        except OSError:
            time.sleep(0.1)

    yield service

    service.terminate()
