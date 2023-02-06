#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import main
import test_business_offer


'''
Теперь дефолтная версия гиперлокальности 3 или 4.
Этот тест оставлен до момента полного удаления гиперлокальности версии 1.
'''


class T(test_business_offer.T):
    @classmethod
    def beforePrepare(cls):
        cls.settings.default_search_experiment_flags += ['market_hyperlocal_context_mmap_version=1']


if __name__ == '__main__':
    main()
