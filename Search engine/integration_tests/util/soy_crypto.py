from distutils.core import setup, Extension


setup(
    name='clemmer',
    version=os.environ['VERSION_FROM_CHANGELOG'],
    description='Python binding for libclemmer.',
    author='Evgenii Romanov',
    author_email='romanoved@yandex-team.ru',
    cmdclass={'build_ext': CopyBuild},
    ext_modules = [
        Extension(
            'clemmer', [os.environ['PY_CLEMMER_SO']],
        )
    ],
)



sfc_module = Extension('soy_encrypt', sources=['soy_encrypt.cpp'])

setup(name='superfastcode', version='1.0',
              description='Python Package with superfastcode C++ extension',
                    ext_modules=[sfc_module]
                          )


def SoyEncrypt(public_key=None, message=None):
    assert public_key is not None
    assert message is not None

    return ""
