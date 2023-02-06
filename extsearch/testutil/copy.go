package testutil

import (
	"fmt"
	"os"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/util"
)

func MustCopyBin(from, to string) {
	err := util.CopyFile(from, to)
	if err != nil {
		panic(fmt.Errorf("copy bin %s -> %s failed: %v", from, to, err))
	}

	err = os.Chmod(to, 0700)
	if err != nil {
		panic(fmt.Errorf("chmod on %s failed: %v", to, err))
	}
}
