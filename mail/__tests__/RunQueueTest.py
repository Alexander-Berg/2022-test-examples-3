import unittest

from run_queue import RunQueue
from run import Run


class RunQueueTest(unittest.TestCase):

    def setUp(self):
        self.run_queue = RunQueue()

    def test_add_to_empty(self):
        """Добавление рана в пустую очередь"""
        self.run_queue.add(Run("2"))
        self.assertEqual(
            self.run_queue.get_next()["priority"],
            2,
            'Ран не добавился'
        )

    def test_add_different_priorities(self):
        """Должны добавлять раны в очередь согласно их приоритету"""
        self.run_queue.add(Run("2"))
        self.run_queue.add(Run("1"), 1)
        self.run_queue.add(Run("3"), 3)
        self.assertEqual(
            1,
            self.run_queue.get_next()["priority"],
            'Первым в очереди должен быть ран с высшим приоритетом 1!'
        )
        self.assertEqual(
            2,
            self.run_queue.get_next()["priority"],
            'Вторым в очереди должен быть ран с приоритетом 2!'
        )
        self.assertEqual(
            3,
            self.run_queue.get_next()["priority"],
            'Последним в очереди должен быть ран с низшим!'
        )

    def test_get_all(self):
        """Получение всех ранов из очереди"""
        self.run_queue.add(Run("2"))
        self.run_queue.add(Run("1"), 1)
        self.assertEqual(
            ["1", "2"],
            [run_q.args for run_q in self.run_queue.get_all()],
            'Не получилось выгрузить всю очередь!'
        )

    def test_size(self):
        """Должны корректно определить размер текущей очереди"""
        self.assertEqual(
            0,
            self.run_queue.size(),
            'Размер пустой очереди должен быть равен 0'
        )
        self.run_queue.add(Run("2"))
        self.run_queue.add(Run("1"), 1)
        self.assertEqual(
            2,
            self.run_queue.size(),
            'Некорректно вычислили размер непустой очереди'
        )

    def test_delete(self):
        """Должны правильно удалить ран из очереди"""
        self.run_queue.add(Run("2"))
        self.run_queue.add(Run("1"), 1)
        self.run_queue.add(Run("3"), 3)
        self.run_queue.delete_run(self.run_queue.get_all()[1].run_id)
        self.assertEqual(
            ["1", "3"],
            [runQ.args for runQ in self.run_queue.get_all()],
            'Ран удалён некорректно'
        )
