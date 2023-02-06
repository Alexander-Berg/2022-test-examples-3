package main

import (
    "fmt"
    "io/ioutil"
    //"regexp"
    "strings"
    "os"
    "log"
    "time"
    "crypto/tls"

    "github.com/jackc/pgx"

    "github.com/yandex/pandora/core"
    "github.com/yandex/pandora/core/aggregator/netsample"
)

type PgAmmo struct {
    Tag         string
    DonorUid    int64
    SourceUid   int64
    FolderName  string
    FolderType  string
}

type PgGun struct {
    conf        PgGunConfig
    connpool    *pgx.ConnPool
    aggr        core.Aggregator
    core.GunDeps
}

type PgGunConfig struct {
    Host        string  `validate:"required"`
    Port        uint16  `validate:"required"`
    Database    string  `validate:"required"`
    User        string  `validate:"required"`
    Password    string
}

func NewPgGun(conf PgGunConfig) *PgGun {
    return &PgGun{conf: conf}
}

func (g *PgGun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
    //password := os.Getenv("DB_PASS")
    pwd_bytes, e := ioutil.ReadFile("pg_pass")
    if e != nil {
        fmt.Println("Unable to read password for DB ", e)
        log.Fatal(e)
    }
    password := strings.TrimSpace(string(pwd_bytes))
    var err error
    //mutex.Lock()
    pgxConfig := pgx.ConnConfig{
        Host:               g.conf.Host, // host (e.g. localhost) or path to unix domain socket directory (e.g. /private/tmp)
        Port:               g.conf.Port,   // default: 5432
        Database:           g.conf.Database,
        User:               g.conf.User,
        Password:           password,
        TLSConfig:          &tls.Config{InsecureSkipVerify: true}, // config for TLS connection -- nil disables TLS
        PreferSimpleProtocol: true,
    }
    pgxConnPoolConfig := pgx.ConnPoolConfig{pgxConfig, 1, nil, 1000000 * time.Nanosecond}
    g.connpool, err = pgx.NewConnPool(pgxConnPoolConfig)
    //mutex.Unlock()
    if err != nil {
        fmt.Println("Unable to establish connection: ", err)
        log.Fatal(err)
    }
    g.aggr = aggr
    g.GunDeps = deps
    return nil
}

func (g *PgGun) Shoot(ammo core.Ammo) {
    customAmmo := ammo.(*PgAmmo) // Shoot will panic on unexpected ammo type. Panic cancels shooting.
    g.shoot(customAmmo)
}

func (g *PgGun) shoot(ammo *PgAmmo) {
    code := 0

//    DB_REQ := "SELECT extract(epoch from now())::int"

DUPLICATE_MAIL := `SELECT COUNT(*) FROM
(select code.store_message($2, coords, hdrs, recipients, attaches, lids, treads, info, mime)
    from (
    select uid,
        (fid,NULL,FALSE,FALSE,st_id,clock_timestamp(),size,attributes,pop_uidl)::code.store_coordinates as coords,
        (subject, firstline, hdr_date, hdr_message_id, extra_data)::code.store_headers as hdrs,
        recipients::text::code.store_recipient[],
        cast_code_mail_attaches_to_store(attaches) as attaches,
        array[]::code.unique_lids lids,
        ('force-new-thread'::mail.threads_merge_rules, NULL, NULL, NULL, NULL, NULL, NULL)::code.store_threading as treads,
        ('xeno_load','xeno_load')::code.request_info as info,
        mime::code.store_mime_part[]
    from 
        (select * from mail.messages where uid = $1 order by mid limit 1 offset floor(random()*50)::int ) src,
        (select fid from code.get_or_create_folder($1, 'Inbox', NULL, 'inbox')) fids
    ) y
) z`

// Select random email from first 50 emails of user $1 and store it's copy to user $2 mailbox


    sample := netsample.Acquire(ammo.Tag)

    var result int64

    err := g.connpool.QueryRow(DUPLICATE_MAIL, ammo.DonorUid, ammo.SourceUid).Scan(&result)
    if err != nil {
        code = 0
        fmt.Println(err)
        if pgerr, ok := err.(pgx.PgError); ok {
            fmt.Fprintf(os.Stderr, "Unexpected postgres error while duplicating message: %v\n", pgerr)
        }
    }

    if result == 1 {
        //fmt.Fprintf(os.Stderr, "Successfull request, %d new mail\n", result)
        code = 200
    } else {
        fmt.Fprintf(os.Stderr, "Something strange, %d new mails\n", result )
        code = 666
    }
    defer func() {
        sample.SetProtoCode(code)
        g.aggr.Report(sample)
    }()
}

