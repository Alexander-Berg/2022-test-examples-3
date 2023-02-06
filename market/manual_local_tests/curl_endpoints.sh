#!/bin/bash

# warehouse
curl -i -X GET "http://localhost:9000/api/rest/v1/warehouse" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/warehouse?name=no_warehouse" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/warehouse?short_name=no-wh" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/warehouse?city=no-city&short_name=no-wh" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/warehouse/1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/warehouse/100500" # 404
curl -i -X POST --data '{"name": 300}' "http://localhost:9000/api/rest/v1/warehouse" # 400
curl -i -X POST --data '{"name": "test-sof", "oebs_location_segment3": "TEST-SOF", "cups_host": "wms-app01sof.market.yandex.net", "conductor_group_id": 434175, "ws_net_prefix": "2a02:6b8:1::/56", "pr_net_prefix": "2a02:5a8:1::/56", "tmp_net_prefix": "2a02:7d8:1::/56", "city":"Sofino", "short_name":"sof", "ws_zombie": "ws_zombie_test", "pr_zombie": "pr_zombie_test", "timezone": "Europe/Kaliningrad", "tsd_zombie": "tsd_zombie_test", "object_type": "FFC", "tsd_net_prefixes": "2a02:5a8:1::/56,2a02:7d8:1::/56"}' "http://localhost:9000/api/rest/v1/warehouse" # 201
curl -i -X PATCH --data '{"timezone": "Asia/Irkutsk"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"cups_host": "wms-app02sof.market.yandex.net"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"name": "tested_twice"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"short_name": "tested"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"object_type": "curve_object_type"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 400
curl -i -X PATCH --data '{"object_type": "SC"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"object_type": "FFC"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"ws_net_prefix": "curveCIDR"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 400
curl -i -X PATCH --data '{"ws_net_prefix": "2a02:7c8:1::/56"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"pr_net_prefix": "curveCIDR"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 400
curl -i -X PATCH --data '{"pr_net_prefix": "2a02:8d8:1::/56"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"tmp_net_prefix": "curveCIDR"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 400
curl -i -X PATCH --data '{"tmp_net_prefix": "2a02:4f8:1::/56"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"oebs_location_segment3": "ZALUNNAYA"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"tsd_net_prefixes": ""}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"tsd_net_prefixes": "2a02:5a8:1::/56,curveCIDR,2a02:7d8:1::/56"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 400
curl -i -X PATCH --data '{"tsd_net_prefixes": "2a02:5a8:1::/56,2a02:7d8:1::/56"}' "http://localhost:9000/api/rest/v1/warehouse/2" # 204
curl -i -X PATCH --data '{"name": "delete_me_please", "ws_net_prefix": "delete_me_please", "city":"delete_me_please", "short_name":"DELETE!!!" }' "http://localhost:9000/api/rest/v1/warehouse/5" # 400
curl -i -X DELETE "http://localhost:9000/api/rest/v1/warehouse/2" # 400
curl -i -X DELETE "http://localhost:9000/api/rest/v1/warehouse/5" # 204
curl -i -X DELETE "http://localhost:9000/api/rest/v1/warehouse/100500" # 404

# type_workstation
curl -i -X GET "http://localhost:9000/api/rest/v1/type_workstation" # 200
curl -i -X POST --data '{"name": "qtest", "short_name": "qtest", "description": "find me please by pattern"}' "http://localhost:9000/api/rest/v1/type_workstation" # 201
curl -i -X GET "http://localhost:9000/api/rest/v1/type_workstation?name=test" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/type_workstation?description=please" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/type_workstation/1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/type_workstation/200" # 404
curl -i -X POST --data '{"uid": 300}' "http://localhost:9000/api/rest/v1/type_workstation" # 400
curl -i -X POST --data '{"name": "change_me_please", "short_name": "test", "description": "Change me please"}' "http://localhost:9000/api/rest/v1/type_workstation" # 201
curl -i -X PATCH --data '{"privilege_level": 100500}' "http://localhost:9000/api/rest/v1/type_workstation/2" # 400
curl -i -X PATCH --data '{"name": "change_me"}' "http://localhost:9000/api/rest/v1/type_workstation/2" # 204
curl -i -X PATCH --data '{"description": "Change me"}' "http://localhost:9000/api/rest/v1/type_workstation/2" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/type_workstation/2" # 200
curl -i -X PATCH --data '{"name": "delete_me", "description": "Delete me"}' "http://localhost:9000/api/rest/v1/type_workstation/2" # 204
curl -i -X DELETE "http://localhost:9000/api/rest/v1/type_workstation/2" # 204
curl -i -X DELETE "http://localhost:9000/api/rest/v1/type_workstation/5" # 404
curl -i -X GET "http://localhost:9000/api/rest/v1/type_workstation/2" # 404
curl -i -X DELETE "http://localhost:9000/api/rest/v1/type_workstation/200" # 404

# model_workstation
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation" # 200
curl -i -X POST --data '{"name": "test", "vendor": "test", "description": "find me please by pattern"}' "http://localhost:9000/api/rest/v1/model_workstation" # 201
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation?name=test" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation?vendor=test" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation?description=please" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation/1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation/200" # 404
curl -i -X POST --data '{"uid": 300}' "http://localhost:9000/api/rest/v1/model_workstation" # 400
curl -i -X POST --data '{"name": "change_me_please", "vendor": "test", "description": "Change me please"}' "http://localhost:9000/api/rest/v1/model_workstation" # 201
curl -i -X PATCH --data '{"privilege_level": 100500}' "http://localhost:9000/api/rest/v1/model_workstation/2" # 400
curl -i -X PATCH --data '{"name": "change_me"}' "http://localhost:9000/api/rest/v1/model_workstation/2" # 204
curl -i -X PATCH --data '{"description": "Change me"}' "http://localhost:9000/api/rest/v1/model_workstation/2" # 204
curl -i -X PATCH --data '{"vendor": "change_me"}' "http://localhost:9000/api/rest/v1/model_workstation/2" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation/2" # 200
curl -i -X PATCH --data '{"name": "delete_me", "vendor": "delete_me", "description": "Delete me"}' "http://localhost:9000/api/rest/v1/model_workstation/2" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation/5" # 200
curl -i -X DELETE "http://localhost:9000/api/rest/v1/model_workstation/5" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_workstation/5" # 404
curl -i -X DELETE "http://localhost:9000/api/rest/v1/model_workstation/200" # 404

# model_printer
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer" # 200
curl -i -X POST --data '{"name": "test", "short_name": "test", "vendor": "test", "description": "find me please by pattern"}' "http://localhost:9000/api/rest/v1/model_printer" # 201
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer?name=test" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer?vendor=test" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer?description=please" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer/1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer/200" # 404
curl -i -X POST --data '{"uid": 300}' "http://localhost:9000/api/rest/v1/model_printer" # 400
curl -i -X POST --data '{"name": "change_me_pleaseqqq", "short_name": "qtestqqq", "vendor": "test", "type_wms": 246, "description": "Change me please", "ppd": "xerox/xrxb310", "protocol": "lpd", "role": "mfp", "ipv4_only": false}' "http://localhost:9000/api/rest/v1/model_printer" # 201
curl -i -X PATCH --data '{"privilege_level": 100500}' "http://localhost:9000/api/rest/v1/model_printer/2" # 400
curl -i -X PATCH --data '{"name": "change_me"}' "http://localhost:9000/api/rest/v1/model_printer/2" # 204
curl -i -X PATCH --data '{"description": "Change me"}' "http://localhost:9000/api/rest/v1/model_printer/2" # 204
curl -i -X PATCH --data '{"vendor": "change_me"}' "http://localhost:9000/api/rest/v1/model_printer/2" # 204
curl -i -X PATCH --data '{"ppd": "xrxb400"}' "http://localhost:9000/api/rest/v1/model_printer/2" # 204
curl -i -X PATCH --data '{"protocol": "ipp"}' "http://localhost:9000/api/rest/v1/model_printer/2" # 204
curl -i -X PATCH --data '{"role": "ipp"}' "http://localhost:9000/api/rest/v1/model_printer/2" # 400
curl -i -X PATCH --data '{"role": "label"}' "http://localhost:9000/api/rest/v1/model_printer/2" # 204
curl -i -X PATCH --data '{"ipv4_only": false}' "http://localhost:9000/api/rest/v1/model_printer/2" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer/2" # 200
curl -i -X PATCH --data '{"name": "delete_me", "vendor": "delete_me", "description": "Delete me"}' "http://localhost:9000/api/rest/v1/model_printer/2" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer/4" # 200
curl -i -X DELETE "http://localhost:9000/api/rest/v1/model_printer/4" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_printer/4" # 404
curl -i -X DELETE "http://localhost:9000/api/rest/v1/model_printer/200" # 404

# model_scanner
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner" # 200
curl -i -X POST --data '{"name": "test", "vendor": "test", "description": "find me please by pattern"}' "http://localhost:9000/api/rest/v1/model_scanner" # 201
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner?name=test" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner?vendor=test" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner?description=please" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner/1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner/200" # 404
curl -i -X POST --data '{"uid": 300}' "http://localhost:9000/api/rest/v1/model_scanner" # 400
curl -i -X POST --data '{"name": "change_me_please", "vendor": "test", "description": "Change me please"}' "http://localhost:9000/api/rest/v1/model_scanner" # 201
curl -i -X PATCH --data '{"privilege_level": 100500}' "http://localhost:9000/api/rest/v1/model_scanner/2" # 400
curl -i -X PATCH --data '{"name": "change_me"}' "http://localhost:9000/api/rest/v1/model_scanner/2" # 204
curl -i -X PATCH --data '{"description": "Change me"}' "http://localhost:9000/api/rest/v1/model_scanner/2" # 204
curl -i -X PATCH --data '{"vendor": "change_me"}' "http://localhost:9000/api/rest/v1/model_scanner/2" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner/4" # 200
curl -i -X PATCH --data '{"name": "delete_me", "vendor": "delete_me", "description": "Delete me"}' "http://localhost:9000/api/rest/v1/model_scanner/2" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner/4" # 200
curl -i -X DELETE "http://localhost:9000/api/rest/v1/model_scanner/4" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/model_scanner/4" # 404
curl -i -X DELETE "http://localhost:9000/api/rest/v1/model_scanner/200" # 404

# workstation
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation?name=default" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation?mac_address=ac:1f:6b" # 400
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation?mac_address=ac:1f:6b:4a:ef:0a" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation?wireless=0" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation?warehouse_id=3" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation?warehouse_id=0,1,2" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation?warehouse_id=0,1,2&type_id=0,1,2&model_workstation_id=0,1,2" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation/1" # 200
curl -i -X POST --data '{"something": "garbage"}' "http://localhost:9000/api/rest/v1/workstation" # 400
curl -i -X POST --data '{"position": 1, "serial_number": "new", "inventory_number": "new", "wireless": "DA", "mac_address": "AC:1F:6B:4A:EB:0F", "status": "active", "warehouse_id": 0, "type_id": 1, "model_workstation_id": 1}' "http://localhost:9000/api/rest/v1/workstation" # 400
curl -i -X POST --data '{"position": 1, "serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC1F.6B4A.EB0F", "status": "active", "warehouse_id": 0, "type_id": 1, "model_workstation_id": 1}' "http://localhost:9000/api/rest/v1/workstation" # 201
curl -i -X POST --data '{"position": 1, "serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "status": "something_invalid_test", "warehouse_id": 0, "type_id": 1, "model_workstation_id": 1}' "http://localhost:9000/api/rest/v1/workstation" # 400
curl -i -X POST --data '{"position": 1, "serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "status": "active", "warehouse_id": 100500, "type_id": 1, "model_workstation_id": 1}' "http://localhost:9000/api/rest/v1/workstation" # 400
curl -i -X POST --data '{"position": 1, "serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "status": "active", "warehouse_id": 0, "type_id": 100500, "model_workstation_id": 1}' "http://localhost:9000/api/rest/v1/workstation" # 400
curl -i -X POST --data '{"position": 1, "serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "status": "active", "warehouse_id": 0, "type_id": 1, "model_workstation_id": 100500}' "http://localhost:9000/api/rest/v1/workstation" # 400
curl -i -X POST --data '{"position": 1, "serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "status": "active", "warehouse_id": 0, "type_id": 1, "model_workstation_id": 1}' "http://localhost:9000/api/rest/v1/workstation" # 409
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation/3" # 200
curl -i -X PATCH --data '{"position": 5}' "http://localhost:9000/api/rest/v1/workstation/3" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation/3" # 200
curl --cookie "Session_id=invalid" -i -X DELETE "http://localhost:9000/api/rest/v1/workstation/3" # 403
curl -i -X DELETE "http://localhost:9000/api/rest/v1/workstation/3" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/workstation/3" # 404
curl -i -X DELETE "http://localhost:9000/api/rest/v1/workstation/3" # 404

# printer
curl -i -X GET "http://localhost:9000/api/rest/v1/printer" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?name=default" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?without_workstation=1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?workstation_id=0,1,2,3" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?mac_address=ac:1f:6b" # 400
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?mac_address=bc:1f:7b:40:ef:20" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?wireless=0" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?warehouse_id=3" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?warehouse_id=0,1,2" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer?warehouse_id=0,1,2&model_printer_id=0,1,2" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/printer/1" # 200
curl -i -X POST --data '{"something": "garbage"}' "http://localhost:9000/api/rest/v1/printer" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "label_size_wms": 7,  "warehouse_id": 0, "workstation_id": 1, "model_printer_id": 1}' "http://localhost:9000/api/rest/v1/printer" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "label_size_wms": 7, "warehouse_id": 1, "workstation_id": 1, "model_printer_id": 100500}' "http://localhost:9000/api/rest/v1/printer" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "label_size_wms": 7, "warehouse_id": 1, "workstation_id": 100500, "model_printer_id": 1}' "http://localhost:9000/api/rest/v1/printer" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "label_size_wms": 7, "warehouse_id": 100500, "workstation_id": 1, "model_printer_id": 1}' "http://localhost:9000/api/rest/v1/printer" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC1F:6B4A:EB0F", "label_size_wms": 7, "warehouse_id": 1, "workstation_id": 1, "model_printer_id": 1}' "http://localhost:9000/api/rest/v1/printer" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "wireless": "DA", "mac_address": "AC:1F:6B:4A:EB:0F", "label_size_wms": 7, "warehouse_id": 1, "workstation_id": 1, "model_printer_id": 1}' "http://localhost:9000/api/rest/v1/printer" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "label_size_wms": 9, "warehouse_id": 1, "workstation_id": 1,  "model_printer_id": 1}' "http://localhost:9000/api/rest/v1/printer" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0F", "label_size_wms": 7, "warehouse_id": 1, "workstation_id": 1,  "model_printer_id": 1}' "http://localhost:9000/api/rest/v1/printer" # 201
curl -i -X POST --data '{"serial_number": "with-ip", "inventory_number": "with-ip", "wireless": false, "mac_address": "AC:1F:6B:4A:EB:0E", "label_size_wms": 7, "ip": "169.254.88.1", "warehouse_id": 1, "workstation_id": 1, "model_printer_id": 1}' "http://localhost:9000/api/rest/v1/printer" # 201
curl -i -X GET "http://localhost:9000/api/rest/v1/printer/3" # 200
curl -i -X PATCH --data '{"serial_number": "delete-me-please"}' "http://localhost:9000/api/rest/v1/printer/3" # 201
curl -i -X PATCH --data '{"workstation_unset": true}' "http://localhost:9000/api/rest/v1/printer/13" # 201
curl -i -X PATCH --data '{"workstation_unset": true}' "http://localhost:9000/api/rest/v1/printer/13" # 201
curl -i -X GET "http://localhost:9000/api/rest/v1/printer/3" # 200
curl -i -X DELETE "http://localhost:9000/api/rest/v1/printer/3" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/printer/3" # 404
curl -i -X DELETE "http://localhost:9000/api/rest/v1/printer/300" # 404

# scanner
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner?serial_number=default" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner?without_workstation=1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner?workstation_id=0,1,2,3" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner?warehouse_id=3" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner?warehouse_id=0,1,2" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner?warehouse_id=0,1,2&model_scanner_id=0,1,2" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner/1" # 200
curl -i -X POST --data '{"something": "garbage"}' "http://localhost:9000/api/rest/v1/scanner" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "warehouse_id": 0, "workstation_id": 1, "model_scanner_id": 1}' "http://localhost:9000/api/rest/v1/scanner" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "warehouse_id": 100500, "workstation_id": 1, "model_scanner_id": 1}' "http://localhost:9000/api/rest/v1/scanner" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "warehouse_id": 1, "workstation_id": 100500, "model_scanner_id": 1}' "http://localhost:9000/api/rest/v1/scanner" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "warehouse_id": 1, "workstation_id": 1, "model_scanner_id": 100500}' "http://localhost:9000/api/rest/v1/scanner" # 400
curl -i -X POST --data '{"serial_number": "new", "inventory_number": "new", "warehouse_id": 0, "model_scanner_id": 1}' "http://localhost:9000/api/rest/v1/scanner" # 201
curl -i -X POST --data '{"serial_number": "qnew", "inventory_number": "qnew", "warehouse_id": 0, "model_scanner_id": 1, "workstation_id": 2}' "http://localhost:9000/api/rest/v1/scanner" # 400
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner/3" # 200
curl -i -X PATCH --data '{"serial_number": "delete-me.please.com"}' "http://localhost:9000/api/rest/v1/scanner/3" # 204
curl -i -X PATCH --data '{"workstation_unset": true}' "http://localhost:9000/api/rest/v1/scanner/3" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner/3" # 200
curl -i -X DELETE "http://localhost:9000/api/rest/v1/scanner/3" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/scanner/3" # 404
curl -i -X DELETE "http://localhost:9000/api/rest/v1/scanner/3" # 404

# work_page
curl -i -X GET "http://localhost:9000/api/rest/v1/work_page" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/work_page/1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/work_page?warehouse_id=0,1,2&type_id=0,3,4" # 200
curl -i -X POST --data '{"something": "garbage"}' "http://localhost:9000/api/rest/v1/work_page" # 400
curl -i -X POST --data '{"url": "https://wms.test.ya.ru", "warehouse_id": 0, "type_id": 1}' "http://localhost:9000/api/rest/v1/work_page" # 201
curl -i -X GET "http://localhost:9000/api/rest/v1/work_page/1" # 200
curl -i -X DELETE "http://localhost:9000/api/rest/v1/work_page/1" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/work_page/1" # 404

# default_flag
curl -i -X GET "http://localhost:9000/api/rest/v1/default_flag" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/default_flag/1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/default_flag?type_id=0,1,2,3" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/default_flag?flag=flag1" # 200
curl -i -X POST --data '{"something": "garbage"}' "http://localhost:9000/api/rest/v1/default_flag" # 400
curl -i -X POST --data '{"flag": "flag1", "type_id": 1}' "http://localhost:9000/api/rest/v1/default_flag" # 409
curl -i -X POST --data '{"flag": "flagtst", "type_id": 100500}' "http://localhost:9000/api/rest/v1/default_flag" # 400
curl -i -X POST --data '{"flag": "flagtst", "type_id": 1}' "http://localhost:9000/api/rest/v1/default_flag" # 201
curl -i -X GET "http://localhost:9000/api/rest/v1/default_flag/3" # 200
curl -i -X DELETE "http://localhost:9000/api/rest/v1/default_flag/3" # 204
curl -i -X GET "http://localhost:9000/api/rest/v1/default_flag/3" # 404
curl -i -X DELETE "http://localhost:9000/api/rest/v1/default_flag/300" # 404

# configuration_flag
curl -i -X GET "http://localhost:9000/api/rest/v1/configuration_flag" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/configuration_flag/1" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/configuration_flag?workstation_id=0,1,2,3" # 200
curl -i -X GET "http://localhost:9000/api/rest/v1/configuration_flag?flag=flag1" # 200
curl -i -X POST --data '{"something": "garbage"}' "http://localhost:9000/api/rest/v1/configuration_flag" # 400
curl -i -X POST --data '{"flag": "flag1", "workstation_id": 6}' "http://localhost:9000/api/rest/v1/configuration_flag" # 409
curl -i -X POST --data '{"flag": "flagtst", "workstation_id": 100500}' "http://localhost:9000/api/rest/v1/configuration_flag" # 400
curl -i -X POST --data '{"flag": "flagtstqq", "workstation_id": 6}' "http://localhost:9000/api/rest/v1/configuration_flag" # 409
curl -i -X GET "http://localhost:9000/api/rest/v1/configuration_flag/4" # 200

# current_user
curl -i "http://localhost:9000/api/rest/v1/current_user" # 200

# other
curl -i "http://localhost:9000/api/other/workstation/mac" # 200
curl -i "http://localhost:9000/api/other/printer/mac"     # 200
curl -i "http://localhost:9000/api/other/printer/mac?location=defwh"     # 200
curl -i "http://localhost:9000/api/other/printer/mac?mac_address=bc:1f:7b:40:ef:24"     # 200
curl -i "http://localhost:9000/api/other/printer/cups"    # 200
curl -i "http://localhost:9000/api/other/dns/wh"          # 200

# idm
curl -i -X GET "http://localhost:9000/api/idm/v1/info/"
curl -i -X GET "http://localhost:9000/api/idm/v1/get-all-roles/"
curl -i -X POST --data-urlencode "login=frodo" \
    --data-urlencode "uid=1120000000348444" \
    --data-urlencode 'role={"nucadmin": "global_params_writer"}' \
    --data-urlencode "path=/nucadmin/global_params_writer" \
    "http://localhost:9000/api/idm/v1/add-role/"

curl -i -X GET "http://localhost:9000/api/idm/v1/get-all-roles/"
curl -i -X POST --data-urlencode "login=frodo" \
    --data-urlencode "uid=1120000000348444" \
    --data-urlencode 'role={"nucadmin": "global_params_writer"}' \
    --data-urlencode "path=/nucadmin/global_params_writer" \
    "http://localhost:9000/api/idm/v1/remove-role/"
