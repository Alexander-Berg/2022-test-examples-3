import socket


def check_port(port):
    handle = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        handle.connect(("127.0.0.1", port))
    except:
        return True
    finally:
        handle.close()
    return False


def find_free_port(reserved_ports):
    for _ in range(len(reserved_ports) + 1):
        free_port = get_free_port()
        if free_port not in reserved_ports:
            return free_port
    raise RuntimeError("all ports are occupied")


def get_free_port():
    handle = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    handle.bind(("", 0))
    port = handle.getsockname()[1]
    handle.close()
    return port
