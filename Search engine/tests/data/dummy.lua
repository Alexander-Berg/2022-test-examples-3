instance = {
    workers = workers;
    maxconn = worker_maxconn;
    log = child_log or '/logs/current-child-log-rpslimiter';

    unistat = {
        addrs = {
            { ip = "localhost"; port = unistat_port; };
        };
        hide_legacy_signals = true;
    };

    addrs = {
        { ip = "::"; port = listening_port; };
    }; -- addrs

    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    rpslimiter_instance = {
        quotas = {
            ["dummy-default"] = { quota = 10; };
        };
        remote = {
            local_host_id = 'localhost';
            sync_path = "/state.sync";
            sync_interval = '100ms';
            localhost = {
                proxy = { host = 'localhost'; port = listening_port; };
            };
        };

        module = {
            errorlog = {
                log = error_log or "/logs/current-error-log-rpslimiter";

                http = {
                    maxlen = 65536; maxreq = 65536;

                    accesslog = {
                        log = access_log or "/logs/current-access-log-rpslimiter";

                        regexp = {
                            ping = {
                                priority = 100;
                                match_and = {
                                    { match_method = { methods = {"get"}; }; };
                                    { match_fsm = { path = "/ping"; }; };
                                };
                                errordocument = {
                                    status = 200;
                                    content = "pong";
                                }; -- errordocument
                            }; -- ping
                            get = {
                                priority = 1;
                                match_method = { methods = {"get"}; };
                                shared = {
                                    uuid = "limiter_router";
                                    regexp = {
                                        ["dummy"] = {
                                            match_fsm = {
                                                header = {
                                                    name = "x-rpslimiter-balancer";
                                                    value = "dummy";
                                                };
                                            };
                                            quota = {
                                                name = "dummy-default";
                                            };
                                        };
                                    };
                                };
                            }; -- get

                            post = {
                                priority = 1;
                                match_method = { methods = {"post"} };

                                prefix_path_router = {
                                    sync = {
                                        route = "/state.sync";
                                        quota_sync = {};
                                    }; -- sync

                                    acquire = {
                                        route = "/quota.acquire";
                                        unpack = {
                                            shared = {
                                                uuid = "limiter_router";
                                            };
                                        }; -- unpack
                                    }; -- acquire
                                }; -- prefix_path_router
                            }; -- post

                            default = {
                                errordocument = {
                                    status = 405;
                                    content = "Bad method";
                                };
                            }; -- default
                        }; -- regexp

                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- module
    }; -- rpslimiter_instance
}; -- instance

