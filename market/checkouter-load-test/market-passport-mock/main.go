package main

import (
	"fmt"
	"net/url"
	"os"
	"strconv"
	"strings"

	"a.yandex-team.ru/apphost/api/service/go/apphost"
	protoanswers "a.yandex-team.ru/apphost/lib/proto_answers"
	"a.yandex-team.ru/library/go/core/xerrors"
)

const GrpcPortNumEnvVar = "PROSTARTER_LISTEN_GRPC_APP_PORT"

func routeRequest(ctx apphost.Context) error {

	httpRequest := &protoanswers.THttpRequest{}
	err := ctx.GetOnePB("http_request", httpRequest)
	if err != nil {
		return err
	}
	path, err := url.Parse(httpRequest.GetPath())
	if err != nil {
		return err
	}
	if strings.HasPrefix(path.Path, "/blackbox") {
		return processBlackboxRequest(ctx, path)
	}
	return redirectToRealBlackbox(ctx)
}

func processBlackboxRequest(ctx apphost.Context, path *url.URL) error {
	query := path.Query()
	method := query.Get("method")
	switch method {
	case "userinfo":
		return userInfo(ctx, query)

	}

	return redirectToRealBlackbox(ctx)
}

func Run() error {
	port, err := getPortNumber()
	if err != nil {
		return err
	}

	apphost.UseGolovanStats(true)
	apphost.HandleFunc("/route", routeRequest)

	return apphost.ListenAndServe(fmt.Sprintf(":%d", port), nil)
}

func getPortNumber() (int, error) {
	port, err := strconv.Atoi(os.Getenv(GrpcPortNumEnvVar))
	if err != nil {
		return 0, xerrors.Errorf("failed to parse %s: %w", GrpcPortNumEnvVar, err)
	}
	return port, nil
}

func main() {
	if err := Run(); err != nil {
		fmt.Fprintf(os.Stderr, "%+v\n", err)
		os.Exit(1)
	}
}
