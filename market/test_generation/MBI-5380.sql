ALTER TABLE shops_web.test_generation
RENAME COLUMN format TO drp_format
/
ALTER TABLE shops_web.test_generation
MODIFY
(
  drp_format NULL
)
/
ALTER TABLE shops_web.test_generation
RENAME COLUMN processed TO drp_processed
/
ALTER TABLE shops_web.test_generation
MODIFY
(
  drp_processed NULL
)
/
CREATE INDEX shops_web.i_tst_generation_release_time ON shops_web.test_generation
(
  RELEASE_TIME
)
/