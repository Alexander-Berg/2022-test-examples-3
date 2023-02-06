package main

import (
	"context"
	"log"
	"net"
	"os"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc"
	"google.golang.org/grpc/test/bufconn"

	zephyrlib "a.yandex-team.ru/search/zephyr/lib"
	pb "a.yandex-team.ru/search/zephyr/proto"
)

const bufSize = 1024 * 1024

var (
	accessStorage *zephyrlib.AccessStorage
	client        pb.ServiceDiscoveryClient
	clock         clockwork.FakeClock
	keyChecker    *zephyrlib.StorageClientMock
	lis           *bufconn.Listener
	metrics       *zephyrlib.Metrics
	sd            *ServiceDiscovery
	server        *grpc.Server
	updateStorage *zephyrlib.UpdateStorage
)

func getInstance() *pb.Instance {
	return &pb.Instance{
		Fqdn:    "alpha",
		Port:    80,
		Project: "test",
		Stage:   "dev",
		Methods: map[string]*pb.Method{
			"/test.Service/sayHello": {
				Input:   "input-hello",
				Output:  "output-hello",
				Timeout: 3,
				Retries: 1,
			},
			"/test.Service/sayHello2": {
				Input:   "input-hello",
				Output:  "output-hello",
				Timeout: 3,
				Retries: 1,
			},
		},
		Birth: float64(clock.Now().Unix()),
	}
}

func bufDialer(context.Context, string) (net.Conn, error) {
	return lis.Dial()
}

func TestMain(m *testing.M) {
	var err error

	clock = clockwork.NewFakeClockAt(time.Unix(0, 0))

	keyChecker = zephyrlib.NewStorageClientMock()
	keyChecker.SetKey("test-key", "test", "dev")

	accessStorage = zephyrlib.NewAccessStorage(keyChecker)
	updateStorage = zephyrlib.NewUpdateStorageWithClock(clock)
	metrics = zephyrlib.NewMetrics(updateStorage)

	sd = NewServiceDiscovery(metrics, accessStorage, updateStorage)

	lis = bufconn.Listen(bufSize)
	server = grpc.NewServer()
	pb.RegisterServiceDiscoveryServer(server, sd)

	go func() {
		if err := server.Serve(lis); err != nil {
			log.Fatalf("Server exited with error: %v", err)
		}
	}()

	conn, err := grpc.DialContext(context.Background(), "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())

	if err != nil {
		log.Fatal("Failed to dial bufnet", err)
	}
	//noinspection GoUnhandledErrorResult
	defer conn.Close()

	client = pb.NewServiceDiscoveryClient(conn)

	os.Exit(m.Run())
}

func TestServiceDiscovery_ReportHealth(t *testing.T) {
	// Keepalive request without previous full report should result in error.
	_, err := client.ReportHealth(context.Background(), &pb.HealthReport{
		AccessKey: "test-key",
		Instance:  getInstance(),
		Keepalive: true,
	})
	assert.Error(t, err)

	// Full report should be accepted.
	_, err = client.ReportHealth(context.Background(), &pb.HealthReport{
		AccessKey: "test-key",
		Instance:  getInstance(),
	})
	assert.NoError(t, err)

	// Instance should be registered along with its methods.
	update, err := client.GetUpdates(context.Background(), &pb.UpdateFilter{})
	assert.NoError(t, err)
	assert.Equal(t, 1, len(update.Instances))
	assert.Equal(t, 0, len(update.KeepaliveKeys))
	assert.Equal(t, 1, len(update.Methods))
}
