# from wizard_lib.utils.yasm_metric_manager import YasmMetricManager

# import logging
# from mock import Mock, call

# from common.utils.yasmutil import Metric


def test_yasm_metric_manager():
    # return when send signal in separated thread will be done
    '''logger = logging.getLogger('test_logger')
    yasm_metric_manager = YasmMetricManager('test', logger)
    yasm_metric_manager.logger.exception = Mock()

    name = 'test_metric'
    expected = Metric(name, 1, 'ammm')
    yasm_metric_manager.yasm_metric_sender.send_one = Mock()
    yasm_metric_manager.send_one(name)
    assert yasm_metric_manager.yasm_metric_sender.send_one.mock_calls == [call(expected)]

    yasm_metric_manager.yasm_metric_sender.send_one = Mock(side_effect=Exception('test exception'))
    yasm_metric_manager.send_one(name)
    assert yasm_metric_manager.logger.exception.mock_calls == [call('cannot send yasm metric')]
    '''
    pass
