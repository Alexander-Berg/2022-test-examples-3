import argparse
import operator
import pkg_resources
import urllib

import bottle

from rta import core
from rta import settings
from rta import template


def _redirect(url="/"):
    bottle.redirect(url)


def _return(default="/"):
    url = bottle.request.get_header("Referer", default)
    _redirect(url)


@bottle.get("/")
def get():
    registry = core.environment["registry"]
    registry.update()
    table = map(operator.attrgetter("properties"), registry)
    table = sorted(table, key=operator.itemgetter("timestamp"), reverse=True)
    context = {
        "user": bottle.request.get_cookie("user"),
        "is_loading": registry.is_in_loading_state,
        "table": table,
    }
    return template.render("index.html", **context)


@bottle.get("/shutdown-instances")
def shutdown_instances():
    """ Support index reload
    """
    registry = core.environment["registry"]
    registry.shutdown_reports()
    return "\"ok\""


@bottle.get("/start-instances")
def start_instances():
    registry = core.environment["registry"]
    registry.load()
    return "\"ok\""


@bottle.get("/log")
def get_log():
    log = bottle.static_file("/var/log/report-testing-assistant.log", root="/")
    log.set_header("Content-Type", "text/plain")
    return log


@bottle.get("/static/<content:path>")
def get_static_content(content):
    static_content_root = pkg_resources.resource_filename(__name__, "data/static")
    return bottle.static_file(content, root=static_content_root)


@bottle.get("/ticket/<ticket>/binary")
def get_ticket_binary(ticket):
    try:
        return core.get_ticket_binary(ticket)
    except RuntimeError as error:
        bottle.abort(500, error.message)


@bottle.get("/user")
def get_user():
    user = bottle.request.get_cookie("user")
    return template.render("user.html", user=user)


@bottle.post("/ticket/<ticket>/binary/copy")
def copy_ticket_binary(ticket):
    target = bottle.request.params.get("target")
    try:
        if target is None:
            raise RuntimeError("You should specify a target path")
        core.copy_ticket_binary(ticket, target)
    except RuntimeError as error:
        bottle.abort(500, error.message)


@bottle.post("/clean")
def clean():
    core.clean()
    _return()


@bottle.post("/dispatch")
def dispatch():
    pids = map(int, bottle.request.params.getall("pids"))
    action = bottle.request.params.get("action")
    if action == "Terminate":
        core.terminate_processes(pids)
    elif action == "Kill":
        core.kill_processes(pids)
    _return()


@bottle.post("/do")
def do():
    action = urllib.unquote(bottle.request.params["action"]).format(**bottle.request.params)
    bottle.response.status = 307
    bottle.response.set_header("Location", action)


@bottle.post("/process/<pid>/kill")
def kill_process(pid):
    try:
        core.kill_process(int(pid))
    except RuntimeError as error:
        bottle.abort(500, error.message)


@bottle.post("/ticket/<ticket>/binary/launch")
def launch_ticket_binary(ticket):
    user = bottle.request.get_cookie("user")
    if user is None:
        _redirect("/user")
    try:
        core.launch_ticket_binary(user, ticket)
        _return()
    except RuntimeError as error:
        bottle.abort(500, error.message)


@bottle.post("/user/log-in")
def log_user_in():
    user = bottle.request.params.get("name")
    bottle.response.set_cookie("user", user, path="/")
    _redirect()


@bottle.post("/user/log-out")
def log_user_out():
    bottle.response.delete_cookie("user", path="/")
    _return("/user")


@bottle.post("/process/<pid>/terminate")
def terminate_process(pid):
    try:
        core.terminate_process(int(pid))
    except RuntimeError as error:
        bottle.abort(500, error.message)


def main(args):
    configuration = settings.configure(args.configuration)
    core.setup_environment(configuration)
    bottle.run(host="localhost", port=int(configuration["rta.port"]))


if __name__ == "__main__":
    description = "Report Testing Assistance daemon"
    parser = argparse.ArgumentParser(description=description)
    provided_configuration = pkg_resources.resource_filename(__name__, "data/rta.cfg")
    parser.add_argument("-c", "--configuration", default=provided_configuration, help="configuration file")
    args = parser.parse_args()
    main(args)
