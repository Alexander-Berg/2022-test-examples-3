/* syntax version 1 */
$script = @@from yamarec1.udfs import *@@;
$compute_parametric_l1_distance = CustomPython::compute_parametric_l1_distance($script);

SELECT
    $compute_parametric_l1_distance(
        AsList(
            AsTuple("1", 0.1f, Just(0.2f), Just(0.3f))
        ),
        AsList(Just("1")),
        AsList(Just("1"))
    )
;
