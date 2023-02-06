// CopyrightV (c) 2017 Yandex LLC. All rights reserved.
// Use of this source code is governed by a MPL 2.0
// license that can be found in the LICENSE file.
// Author: Vladimir Skipor <skipor@yandex-team.ru>

package main

import (
    "github.com/spf13/afero"
    //  "runtime/debug"
    "github.com/yandex/pandora/cli"
    "github.com/yandex/pandora/components/phttp/import"
    "github.com/yandex/pandora/core"
    "github.com/yandex/pandora/core/import"
    "github.com/yandex/pandora/core/register"
)

func main() {
    // Standard imports.
    fs := afero.NewOsFs()
    coreimport.Import(fs)
    // May not be imported, if you don't need http guns and etc.
    phttp.Import(fs)

    // Custom imports. Integrate your custom types into configuration system.
    coreimport.RegisterCustomJSONProvider("collector_http_provider", func() core.Ammo { return &CollectorAmmo{} })
    coreimport.RegisterCustomJSONProvider("collector_pg_provider", func() core.Ammo { return &PgAmmo{} })

    register.Gun("collector_http_gun", NewHttpGun)
    register.Gun("collector_pg_gun", NewPgGun)

    cli.Run()
}
