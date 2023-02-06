
-- procedures to keep yql code testable
-- they are replaced in tests with mocks

-- allows to mock table calls
DEFINE SUBQUERY $_table($tablename) as
    SELECT * FROM $tablename;
END DEFINE;

$_dir = ($path) -> { return $path; };
