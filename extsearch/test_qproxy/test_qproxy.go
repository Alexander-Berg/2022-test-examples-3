package main

import (
	"a.yandex-team.ru/extsearch/video/station/starter/downloader"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"

	"bufio"
	"os"
	"strings"
)

func main() {
	loggerCfg := zap.JSONConfig(log.TraceLevel)
	logger, _ := zap.New(loggerCfg)

	hmacsecret := os.Getenv("HMAC_SECRET")
	dlProd := downloader.NewDownloader(hmacsecret, "", nil, "")
	dlPres := downloader.NewDownloader(hmacsecret, downloader.EndpointPrestable, nil, "")
	reader := bufio.NewReader(os.Stdin)
	for {
		line, err := reader.ReadString('\n')
		line = strings.TrimSpace(line)
		logger.Info("start processing", log.Any("url", line))
		if line == "" || err != nil {
			logger.Info("global err", log.Any("err", err))
			break
		}
		resURL, err := dlProd.StartDownload(line, "")
		logger.Info("prod", log.Any("url", line), log.Any("err", err), log.Any("resURL", resURL))
		resURL, err = dlPres.StartDownload(line, "")
		logger.Info("pres", log.Any("url", line), log.Any("err", err), log.Any("resURL", resURL))
	}
}
