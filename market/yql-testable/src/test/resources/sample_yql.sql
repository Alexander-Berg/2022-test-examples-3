$yql = (
    select *
    from $_table("//my_simple/table")
    join range($_dir("//my_custom/dir")) --_IMPORT_ -- HACK for faster tests
);
