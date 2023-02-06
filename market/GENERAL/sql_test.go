package sql

import (
	"fmt"
	"testing"
)

func DisabledTestGetFreeInfo(t *testing.T) {
	LocalConnectToDB()

	sku := "SKU1000382362"
	count := 1

	var infos []string

	for i := 0; i < count; i++ {
		infos = append(infos, GetInventoryInfo(freeInfoQuery(sku, infos)).SerialNumber)
	}
	fmt.Printf("Infos = '%v'\n", infos)
}

func freeInfoQuery(sku string, serialNumbers []string) InfoBy {
	params := []interface{}{sku, ""}
	other := ""
	if serialNumbers != nil {
		for _, sn := range serialNumbers {
			params = append(params, sn)
		}
		other = " AND SerialNumber NOT IN(" + genListOfParams(3, len(serialNumbers)) + ")"
	}
	return InfoBy{
		Condition: "SKU = @p1 AND ID = @p2" + other,
		Args:      params,
	}
}
