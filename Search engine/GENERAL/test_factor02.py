from pytest_factor.factortypes.config.factor import Factor
from pytest_factor.factortypes.config.checker import Checker
from pytest_factor.factortypes.config.predefines.checkers import MeanDeltaChecker
from pytest_factor.factortypes.config.predefines.checkers import StrictDiffChecker
from pytest_factor.factortypes.config.predefines.checkers import QueriesCountChecker
from pytest_factor.factortypes.config.predefines.checkers import ActuationPercentageChecker

checker1 = StrictDiffChecker()

checker2 = ActuationPercentageChecker()

checker3 = Checker(name='diff norm',
                   description='this is first test',
                   queryDataProcessor=lambda results: results[0] == results[1] if results[0] and results[1] else None,
                   testFunction=lambda q, b, x: len(filter(lambda x: not x, q)) / float(
                       len(filter(lambda x: x is not None, q))) < 0.25)

checker4 = Checker(name='sum_eq',
                   description='this is first test',
                   betaDataProcessor=lambda results: results,
                   testFunction=lambda q, b, x: sum(b[0]) == sum(b[1]))

checker5 = Checker(name='array_sum',
                   description='this is first test',
                   betaDataProcessor=lambda results: results,
                   testFunction=lambda q, b, x: abs(sum(b[0]) - sum(b[1])) < 100)

factor1 = Factor(name='SUM Source Id In 10',
                 description='not a simple factor',
                 formula=lambda ans: sum([data['id'] for data in ans['searchdata']]),
                 checkers=[checker1, checker2, checker3])

factor2 = Factor(name='SUM src Id In 10',
                 description='not a simple factor',
                 formula=lambda ans: sum([data['id'] for data in ans['searchdata']]),
                 checkers=[MeanDeltaChecker(delta=3), QueriesCountChecker(limit=99)])
