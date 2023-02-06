#!/usr/bin/env bash

cd processors
python -m unittest operations_department_test_1_1 && \
python -m unittest operations_department_test_2_1 && \
python -m unittest operations_department_test_2_2 && \
python -m unittest operations_department_test_3_1 && \
python -m unittest operations_department_test_3_1_2 && \
python -m unittest operations_department_test_3_2 && \
python -m unittest operations_department_test_3_3 && \
python -m unittest common_test_1_1 && \
echo "Ok"
