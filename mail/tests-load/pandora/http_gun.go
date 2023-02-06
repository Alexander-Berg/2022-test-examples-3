// Yandex collectors http gun

package main

import (
    "fmt"
    "io/ioutil"
    "net/http"
    "net/http/httputil"
   // "net/url"
    "regexp"
    "strings"
    "time"
    //"encoding/hex"

    "github.com/yandex/pandora/core"
    "github.com/yandex/pandora/core/aggregator/netsample"
)

type CollectorAmmo struct {
    Tag       string
    SourceUid int64
    TargetUid int64
}

type Payload struct {
    Method string
    Uri    string
    Data   string
    Assert string
}

type HttpGunConfig struct {
    Target string `validate:"required"` // Configuration will fail, without target defined
}

type HttpGun struct {
    // Configured on construction.
    client http.Client
    //expire time.Time
    conf   HttpGunConfig
    // Configured on Bind, before shooting
    aggr core.Aggregator // May be your custom Aggregator.
    core.GunDeps
}

func (g *HttpGun) makeReq(ammo *CollectorAmmo, payload *Payload) *http.Request {
    req, _ := http.NewRequest(payload.Method, strings.Join([]string{"http://", g.conf.Target, payload.Uri}, ""), strings.NewReader(payload.Data))
    req.Header.Add("Content-Type", "application/x-www-form-urlencoded")
    req.Header.Add("X-Real-IP", "213.180.206.57")
    req.Header.Add("User-Agent", "pandora")
    return req
}

func (g *HttpGun) genPayload(ammo *CollectorAmmo) *Payload {
    payload := Payload{
        Method: "POST",
        Assert: "\"status\":\"error\"",
    }
    switch ammo.Tag {
    case "ping":
        payload.Method = "GET"
        payload.Uri = "/ping"
        payload.Assert = "pong"
    case "create_collector":
        payload.Uri = fmt.Sprintf("/api/create?user=new-collector-%[2]d@yandex.ru&mdb=maildb-load&suid=%[2]d&login=new-collector-%[1]d&server=imap.yandex.ru&email=new-collector-%[1]d@yandex.ru&port=993&ssl=1&no_delete_msgs=1&sync_abook=0&mark_archive_read=1", ammo.SourceUid, ammo.TargetUid)
        payload.Data = "password=1234"
        payload.Assert = "\"popid\":"
    case "list":
        payload.Method = "GET"
        payload.Uri = fmt.Sprintf("/api/list?json=1&mdb=pg&suid=%[1]d", ammo.TargetUid)
        payload.Assert = "\"popid\":|\"error\":{\"method\":\"list\",\"reason\":\"syntax error\""

    default:
        fmt.Println("Wrong handler: ", ammo.Tag)
        payload.Method = "GET"
        payload.Uri = "/ping"
        payload.Assert = ""
    }
    return &payload
}

func NewHttpGun(conf HttpGunConfig) *HttpGun {
    return &HttpGun{conf: conf}
}

func (g *HttpGun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
    tr := &http.Transport{
        MaxIdleConns:       1,
        IdleConnTimeout:    600 * time.Second,
        DisableCompression: true,
    }
    g.client = http.Client{Transport: tr} //keep-alive shooting if you want to close then add сlose header and make your server close the connection
    //g.expire = time.Now().AddDate(0, 0, 1)
    g.aggr = aggr
    g.GunDeps = deps
    return nil
}

func (g *HttpGun) Shoot(ammo core.Ammo) {
    customAmmo := ammo.(*CollectorAmmo) // Shoot will panic on unexpected ammo type. Panic cancels shooting.
    g.shoot(customAmmo)
}

func (g *HttpGun) shoot(ammo *CollectorAmmo) {
    code := 0

    payload := g.genPayload(ammo)
    req := g.makeReq(ammo, payload)

    sample := netsample.Acquire(ammo.Tag)
    rs, err := g.client.Do(req)
    if err != nil {
        code = 0
        fmt.Println(err)
    } else {
        code = rs.StatusCode
        if code == 200 {
            respBody, _ := ioutil.ReadAll(rs.Body)
            re := regexp.MustCompile(payload.Assert)
            if payload.Assert != "" && re.FindString(string(respBody)) == "" {
                code = 666
                ErrorMessage := fmt.Sprintf("Error in response to %s :%s, got %s instead of %s", ammo.Tag, payload, string(respBody), payload.Assert)
                fmt.Println(ErrorMessage)
            } else {
                code = rs.StatusCode
            }
        } else {
            respBody, _ := ioutil.ReadAll(rs.Body)
            requestDump, err := httputil.DumpRequest(req, true)
            if err != nil {
                fmt.Println(err)
            }
            ErrorMessage := fmt.Sprintf("HTTP Error %d in response to %s :%s, got %s", rs.StatusCode, ammo.Tag, requestDump, string(respBody))
            fmt.Println(ErrorMessage)
        }
        rs.Body.Close()
    }
    defer func() {
        sample.SetProtoCode(code)
        g.aggr.Report(sample)
    }()
}


