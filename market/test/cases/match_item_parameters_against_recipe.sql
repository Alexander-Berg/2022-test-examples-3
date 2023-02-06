/* syntax version 1 */
$script = @@from yamarec1.udfs import *@@;
$match_item_parameters_against_recipe = CustomPython::match_item_parameters_against_recipe($script);

SELECT 
    $match_item_parameters_against_recipe(
        AsDict(
            AsTuple("1", "9001")
        ),
        "[{\"param_id\": \"0\", \"param_type\": \"BOOLEAN\", \"value_id\": \"1\"}]"
    )
;
