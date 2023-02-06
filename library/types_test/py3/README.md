Helper library to add mypy tests to your project
================================================

What is *mypy*? *mypy* is a tool to validate type annotations in your Python project.

While it works fine with external world Python, we had to make some magic for it to work in Arcadia,
and this is a library to validate your project easily.

How should I do it?
===================

First thing you should know: *mypy* is slow. Despite best efforts its code is overcomplicated at times and just cannot process every single included Python file in a reasonable time. May be in the future… But now you have to select some subdirectory in Arcadia to check.

Second thing comes from the first: *mypy* is not supported natively in Arcadia yet. You should add tests for your project manually.

Imagine, you have a project **myproject** with the following structure:

    arcadia/
            ya.make
            search/
                   ya.make
                   myproject/
                             ya.make
                             bin/
                                 main.py
                                 ya.make
                             component1/
                                 file1.py
                                 file2.py
                                 ya.make
                                 tests/
                                       test_file1.py
                                       ya.make
                            component2/
                                file1.py
                                file2.py
                                ya.make
                            ...

The best idea would be to check the whole `search/myproject/` tree. However you should know: **PY2_PROGRAM targets cannot be linked wth tests!** That's why smart people move all the logic into PY2_LIBRARY and only call very simple `main()` in PY2_PROGRAM main.py

Create test target
------------------

To test the project you should create a new test target, e.g. `search/myproject/tests/mypy/` and put two files there:

### ya.make

    PY3TEST()

    OWNER(g:your_group_name)

    PEERDIR(
        # This target is mandatory, it does all the job
        library/python/testing/types_test/py3

        # These targets are needed to include all the checked files and their
        # dependencies to test binary
        search/myproject/component1
        search/myproject/component2
    )

    TEST_SRCS(
        conftest.py
    )

    # Since mypy is rather slow it could be a good idea to force test
    # to have MEDIUM or even LARGE size, and increase timeout.
    SIZE(MEDIUM)
    TIMEOUT(600)

    END()

### conftest.py

    def mypy_check_root() -> str:
        # Specify path relative to Arcadia root,
        # pointing to subdirectory of your project.
        # Everything else will be autogenerated.

        return 'search/myproject/'

Now just run `ya make -tt search/myproject/tests/mypy` and look at the results. Voilà!

Learn more about *mypy*: [http://www.mypy-lang.org ](http://www.mypy-lang.org/)

TODO
====

Things not supported yet:

* Compiled Python modules. It's possible to autogenerate stubs for them, but it's TBD.
* Protobufs. While `protospec_pb2.py` files are included, they do not provide real interface. mypy-protobuf plugin is not included and not supported yet.
* Custom module stubs. While one can already inject their own stub file into binary as a resource and this would work, it's not a convenient way to do things.