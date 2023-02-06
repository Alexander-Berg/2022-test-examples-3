import os
import yaml

import yatest.common

from search.tools.woland.lib.panels_loader import PanelsLoader

users_panels_path = yatest.common.source_path('search/tools/woland/panels')
test_panels_path = yatest.common.source_path('search/tools/woland/tests/test_panels')


def test_panels_load():
    panels_list = []
    for root, dirs, files in os.walk(users_panels_path):
        for f in files:
            if f.endswith('.yaml'):
                with open(os.path.join(root, f), 'rb') as data:
                    y = yaml.load(data, Loader=yaml.FullLoader)
                    if y.get('.standalone', True) is False:
                        continue
                sub = root.split(users_panels_path, 1)[1].strip('/')
                panels_list.append(os.path.join(sub, f))

    panels_loader = PanelsLoader(users_panels_path)

    for name in [name[:-5] for name in panels_list]:
        try:
            panels_loader.get_panel_config(name)
        except Exception as e:
            print('We got and exception {!s} while loading {}'.format(e, name))
            raise


def test_services_and_imports():
    panels_loader = PanelsLoader(test_panels_path)

    try:
        parent_panel = panels_loader.get_panel_config('parent_panel')
        import_as_service = panels_loader.get_panel_config('import_as_service')
        import_as_include_monitors = panels_loader.get_panel_config('import_as_include_monitors')
        import_as_include_monitors_with_rewrites = panels_loader.get_panel_config('import_as_include_monitors_with_rewrites')
    except Exception as e:
        print('We got and exception {!s} while loading'.format(e))
        raise

    assert(parent_panel.geo == ['sas', 'man', 'vla'])
    assert(import_as_service.geo == ['sas'])
    assert(import_as_include_monitors.geo == ['sas'])
    assert(import_as_include_monitors_with_rewrites.geo == ['sas'])

    assert(parent_panel.ctype == ['prod', 'prestable'])
    assert(import_as_service.ctype == ['ctype_from_imported_panel'])
    assert(import_as_include_monitors.ctype == ['hamster'])
    assert(import_as_include_monitors_with_rewrites.ctype == ['hamster'])

    assert(parent_panel.itype == 'apphost')
    assert(import_as_service.itype == 'itype_from_imported_panel')
    assert(import_as_include_monitors.itype == 'itype_which_will_be_rewritten')
    assert(import_as_include_monitors_with_rewrites.itype == 'itype_which_will_be_rewritten')

    assert(parent_panel.sig_spec[0].geo == ['sas'])
    assert(parent_panel.sig_spec[1].geo == ['sas', 'man', 'vla'])
    assert(parent_panel.sig_spec[2].geo == ['msk', 'vla', 'sas'])
    assert(parent_panel.sig_spec[0].ctype == ['ctype_from_imported_panel'])
    assert(parent_panel.sig_spec[1].ctype == ['prod', 'prestable'])
    assert(parent_panel.sig_spec[2].ctype == ['prod', 'prestable'])

    assert(parent_panel.sig_spec[0].itype == 'itype_from_imported_panel')
    assert(parent_panel.sig_spec[1].itype == 'apphost')
    assert(parent_panel.sig_spec[2].itype == 'apphost')
