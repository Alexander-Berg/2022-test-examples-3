package util

import (
	coreZap "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/yandex/tvm"
	"a.yandex-team.ru/library/go/yandex/tvm/tvmauth"
	"context"
	uberZap "go.uber.org/zap"
)

type TvmConfig struct {
	src    tvm.ClientID
	dst    tvm.ClientID
	secret string
}

func NewTvmConfig(src tvm.ClientID, dst tvm.ClientID, secret string) TvmConfig {
	return TvmConfig{src: src, dst: dst, secret: secret}
}

func GetTvmClient(config TvmConfig) *tvmauth.Client {
	settings := tvmauth.TvmAPISettings{
		SelfID: config.src,
		ServiceTicketOptions: tvmauth.NewIDsOptions(
			config.secret,
			[]tvm.ClientID{
				config.dst,
			},
		)}
	logger := uberZap.L()
	logger.Info("try get tvm client",
		uberZap.Uint32("srcId", uint32(config.src)),
		uberZap.Uint32("dstId", uint32(config.dst)))
	coreLogger := coreZap.Logger{L: logger}
	client, err := tvmauth.NewAPIClient(settings, coreLogger.Logger())
	if err != nil {
		logger.Error("get tvm-client error", uberZap.Error(err))
		panic(err)
	}
	return client
}

func GetAddTicketFunc(tvmSrcID tvm.ClientID, tvmDstID tvm.ClientID, secret string) func(r map[string]string) error {
	tvmClient := GetTvmClient(NewTvmConfig(tvmSrcID, tvmDstID, secret))
	return func(r map[string]string) (err error) {
		ticket, err := tvmClient.GetServiceTicketForID(context.Background(), tvmDstID)
		if err == nil {
			r["X-Ya-Service-Ticket"] = ticket
			return nil
		}
		uberZap.L().Error("GetServiceTicketForID error", uberZap.Error(err),
			uberZap.Uint32("src", uint32(tvmSrcID)),
			uberZap.Uint32("dst", uint32(tvmDstID)))
		return err
	}
}
