import os


def test_unique_routes():
    for meta_route_file_path in _find_file("metaroutes"):
        routes = list(_read_meta_routes(meta_route_file_path))
        assert len(routes) == len(set(routes))
    for route_file_path in _find_file("routes"):
        routes = list(_read_routes(route_file_path))
        assert len(routes) == len(set(routes))


def test_using_routes():
    for route_file_path in _find_file("routes"):
        routes = set(_read_routes(route_file_path))
        route_dir = os.path.dirname(route_file_path)
        used_routes = set(_read_meta_routes(os.path.join(route_dir, "metaroutes")))
        for route in routes:
            assert route in used_routes


def _read_routes(path):
    all_routes = []
    with open(path, "rt") as routes_file:
        for line in routes_file:
            if not line.startswith("#"):
                route, _ = line.strip().split("\t")
                all_routes.append(route.strip())
    return all_routes


def _read_meta_routes(path):
    all_routes = []
    with open(path, "rt") as meta_routes_file:
        for line in meta_routes_file:
            _, routes = line.strip().split("\t")
            all_routes.extend([
                route.strip()
                for route in routes.split(",")
            ])
    return all_routes


def _find_file(file_name):
    for root, dir_names, file_names in os.walk("./data/metahosts"):
        if file_name in file_names:
            yield os.path.join(root, file_name)
