#!/bin/sh
curl -s 'http://report.tst.vs.market.yandex.net:17051/yandsearch?place=prime&cpa=real&rids=213&pp=18&text=iphone&rgb=blue&fesh=431782&numdoc=50' | jq '.search.results[].offers.items[] | {supplierId: .supplier.id, warehouseId: .supplier.warehouseId, shopSku: .shopSku}'
