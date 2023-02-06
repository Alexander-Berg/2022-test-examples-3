import math

from . import secondary

from yamarec1.factories.udf import udf


@udf(["Double", "Double"], "Double")
def hypotenuse(a, b):
    return math.sqrt(secondary.square(a) + secondary.square(b))
