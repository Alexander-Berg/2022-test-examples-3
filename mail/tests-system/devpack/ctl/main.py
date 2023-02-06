def main():
    from apq_tester.root import ApqTesterService

    import sys
    from IPython.core import ultratb
    sys.excepthook = ultratb.FormattedTB(mode='Verbose', color_scheme='Linux', call_pdb=1)

    from mail.devpack.ctl.lib.main import main as run_main
    run_main(deps_root=ApqTesterService)
