
-- mock template

-- allows to mock table calls
DEFINE SUBQUERY $_table($tablename) as
    $target_name = _TABLENAME_;
    SELECT * FROM $target_name with inline;
END DEFINE;

$_dir = ($path) -> {return _DIRNAME_;};
