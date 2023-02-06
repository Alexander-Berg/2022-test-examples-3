Insert into OP_PERM
   (id, op_name)
 Values
   (1, 'testOperation');
Insert into OP_PERM
   (id, op_name)
 Values
   (2, 'allOperations');
Insert into OP_PERM
   (id, op_name)
 Values
   (3, 'otherTestOperation');
Insert into OP_PERM
   (id, op_name)
 Values
   (4, 'forbiddenToAllOperation');
Insert into OP_PERM
   (id, op_name)
 Values
   (5, 'nullAuthoritiesOperation');
COMMIT;

Insert into OP_ALIAS
   (id, op_name, alias)
 Values
   (1, 'testOperation', 'allOperations');
Insert into OP_ALIAS
   (id, op_name, alias)
 Values
   (2, 'otherTestOperation', 'allOperations');
COMMIT;

Insert into PERM_AUTH
   (id, op_perm_id, auth_name)
 Values
   (1, 3, 'YAMANAGER');
Insert into PERM_AUTH
   (id, op_perm_id, auth_name)
 Values
   (2, 1, 'MBI_DEVELOPER');
COMMIT;

Insert into STATIC_AUTH
   (id, user_id, auth_name)
 Values
   (1, 14891462, 'MBI_DEVELOPER');
COMMIT;

