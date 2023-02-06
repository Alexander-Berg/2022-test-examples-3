SELECT object_name,
       object_type,
       owner,
       DECODE(object_type, 'VIEW', 1,
             'MATERIALIZED VIEW', 1,
             'PACKAGE', 3,
             'PACKAGE BODY', 4, 2) AS recompile_order
FROM   ALL_OBJECTS
WHERE  object_type in ('PACKAGE', 'PACKAGE BODY', 'VIEW', 'PROCEDURE', 'FUNCTION', 'SYNONYM', 'MATERIALIZED VIEW')
   AND owner = ?
   AND object_name NOT LIKE 'TO_DELETE_%'
   AND object_name NOT LIKE 'MIGRATION_%'
   AND object_name NOT LIKE 'MIGRATED_%'
