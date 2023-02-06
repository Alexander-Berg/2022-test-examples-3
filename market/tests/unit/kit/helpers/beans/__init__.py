import sys

from yamarec1.kit.helpers import BeanBag


sys.modules[__name__] = BeanBag(sys.modules[__name__])
