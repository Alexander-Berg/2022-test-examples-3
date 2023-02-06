import yatest.common as yatest
import yatest.common.network as network

pseudo_tunneller_ops = yatest.binary_path("search/tunneller/tools/pseudo_tunneller/pseudo_tunneller")
diff_tool = yatest.binary_path("search/tunneller/tools/diff_tool/diff_tool")


def run_test(file_name, test_sensors=False):
    input_file = open(file_name)

    port_manager = network.PortManager()
    port = port_manager.get_port()

    cmd_pseudo = [pseudo_tunneller_ops, "-p{}".format(port)]

    if test_sensors:
        cmd_pseudo.append('--test-sensors')
        return yatest.canonical_execute(cmd_pseudo, stdin=input_file, timeout=30)

    return yatest.canonical_execute(cmd_pseudo, stdin=input_file, timeout=30, diff_tool=diff_tool)


def test_tunneller_1():
    return run_test('requests')


def test_tunneller_sensors():
    return run_test('requests', test_sensors=True)
