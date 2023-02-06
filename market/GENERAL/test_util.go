package routeytclient

import (
	"context"
	"fmt"

	"a.yandex-team.ru/yt/go/schema"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
)

func (ryc RoutesYtController) CreateTestTableYT() error {
	if ryc.ytReplicatedClient == nil {
		return fmt.Errorf("no yt client")
	}
	ctx := context.Background()
	dstSchema := schema.MustInfer(RouteYtRow{})
	for i := range dstSchema.Columns {
		if dstSchema.Columns[i].Name == "RouteID" {
			dstSchema.Columns[i].SortOrder = schema.SortAscending
		}
	}
	err := createDynTableTest(
		ctx,
		ryc.ytReplicatedClient,
		ryc.replicaPath,
		dstSchema,
	)
	if err != nil {
		return err
	}
	return nil
}

func createDynTableTest(
	ctx context.Context,
	ycClient yt.Client,
	replicaPath ypath.YPath,
	dstSchema schema.Schema,
) error {
	replicaTableYtOptions := &yt.CreateNodeOptions{
		IgnoreExisting: true,
		Recursive:      true,
		Attributes: map[string]interface{}{
			"dynamic": true,
			"schema":  dstSchema,
		},
	}
	_, err := ycClient.CreateNode(ctx, replicaPath, yt.NodeTable, replicaTableYtOptions)
	if err != nil {
		return err
	}
	// маунтим реплику
	err = ycClient.MountTable(ctx, replicaPath.YPath(), nil)
	if err != nil {
		return err
	}
	return nil
}
