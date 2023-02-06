import jinja2


environment = jinja2.Environment(loader=jinja2.PackageLoader("rta", "data/templates"))


def render(template_name, **context):
    template = environment.get_template(template_name)
    return template.render(**context)
