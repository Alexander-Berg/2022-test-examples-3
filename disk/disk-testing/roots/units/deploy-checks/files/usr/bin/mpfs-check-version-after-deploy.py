#!/usr/bin/env python
# -*- coding: utf-8 -*-

import socket
import subprocess
import json
import requests

import warnings
warnings.filterwarnings('ignore')

# файл, в который записывается версия устанавливаемого пакета апи перед выполнением
# task do или task preinstall в executer
INSTALLED_BY_EXECUTER_API_VERSION_FILE_PATH = '/var/cache/disk_deploy/python-mpfs-api.txt'
# файл, в который записывается версия устанавливаемого пакета mpfs перед выполнением
# task do или task preinstall в executer
INSTALLED_BY_EXECUTER_DISK_VERSION_FILE_PATH = '/var/cache/disk_deploy/python-mpfs-disk.txt'
# файл, в который записывается версия устанавливаемого пакета очереди перед выполнением
# task do или task preinstall в executer
INSTALLED_BY_EXECUTER_QUEUE_VERSION_FILE_PATH = '/var/cache/disk_deploy/python-mpfs-queue.txt'


MWORKER = 'mworker'
PWORKER = 'pworker'
MPFS = 'mpfs'
API = 'api'
COPYDB = 'copydb'


PRODUCTION_ENV = 'production'
PRESTABLE_ENV = 'prestable'
TESTING_ENV = 'testing'
DEVELOPMENT_ENV = 'testing'


def map_host_env_to_exec_env(env):
    """Преобразовать окружение получение из имени хоста в окружение в котором работает сервис.

    По сути просто преобразует по следующему правилу:
      * disk -> production
      * dsp -> prestable
      * dst -> testing
      * dsd -> development
    """
    if env == 'disk':
        return PRODUCTION_ENV
    elif env == 'dsp':
        return PRESTABLE_ENV
    elif env == 'dst':
        return TESTING_ENV
    elif env == 'dsd':
        return DEVELOPMENT_ENV
    else:
        raise NotImplementedError()


def determine_group_and_environment():
    hostname = socket.gethostname()
    name, environment, _ = hostname.split('.', 2)
    environment = map_host_env_to_exec_env(environment)

    if name.startswith('mworker'):
        group = MWORKER
    elif name.startswith('pworker'):
        group = PWORKER
    elif name.startswith('mpfs'):
        group = MPFS
    elif name.startswith('api'):
        group = API
    elif hostname.startswith('copydb'):
        group = COPYDB
    else:
        raise NotImplementedError()

    return group, environment


GROUP, EXEC_ENVIRONMENT = determine_group_and_environment()


def compare_package_version_by_dpkg(package, version_installed_by_executer_file_path):
    """Сравнить версию пакета в системе с версией записанной в файл при установке через executer."""
    output = subprocess.check_output('dpkg -l | grep %s' % package, shell=True)
    output = output.strip()
    split_result = output.split()
    package_version = split_result[2]
    package_version = package_version.replace('-', '.')

    with open(version_installed_by_executer_file_path) as f:
        installed_by_executer_version = f.read().strip()
        if installed_by_executer_version != package_version:
            print 'Versions are not equal [%s/dpkg]: %s != %s.' % (
                GROUP, installed_by_executer_version, package_version
            )
            exit(1)
        else:
            print 'Versions are equal [%s/dpkg]: %s' % (
                GROUP, installed_by_executer_version
            )


def check_deploy_for_mworker():
    """Проверить что всё поставилось корректно для машины mworker."""
    compare_package_version_by_dpkg('python-mpfs-queue', INSTALLED_BY_EXECUTER_QUEUE_VERSION_FILE_PATH)


def check_deploy_for_pworker():
    """Проверить что всё поставилось корректно для машины pworker."""
    compare_package_version_by_dpkg('python-mpfs-disk', INSTALLED_BY_EXECUTER_DISK_VERSION_FILE_PATH)


def check_deploy_for_mpfs():
    """Проверить что всё поставилось корректно для машины mpfs."""
    compare_package_version_by_dpkg('python-mpfs-disk', INSTALLED_BY_EXECUTER_DISK_VERSION_FILE_PATH)
    compare_disk_version_by_http()


def check_deploy_for_copydb():
    """Проверить что всё поставилось корректно для машины copydb."""
    compare_package_version_by_dpkg('python-mpfs-disk', INSTALLED_BY_EXECUTER_DISK_VERSION_FILE_PATH)


def check_deploy_for_api():
    """Проверить что всё поставилось корректно для машины api."""
    compare_package_version_by_dpkg('python-mpfs-api', INSTALLED_BY_EXECUTER_API_VERSION_FILE_PATH)
    compare_api_version_by_http('http://localhost:8080')
    compare_api_version_by_http('https://localhost')


def compare_disk_version_by_http():
    response = requests.get('http://localhost/version')
    http_version = response.content.replace('"', '')
    with open(INSTALLED_BY_EXECUTER_DISK_VERSION_FILE_PATH) as f:
        installed_version = f.read().strip()
        if http_version == installed_version:
            print 'Versions are equal [disk/http/80]: %s.' % http_version
        else:
            print 'Versions are not equal [disk/http/80]: %s != %s.' % (http_version, installed_version)
            exit(1)


def compare_api_version_by_http(url):
    response = requests.get(url, verify=False)
    http_version = json.loads(response.content)['build']
    with open(INSTALLED_BY_EXECUTER_API_VERSION_FILE_PATH) as f:
        installed_version = f.read().strip()
        if http_version == installed_version:
            print 'Versions are equal [%s]: %s.' % (url, http_version)
        else:
            print 'Versions are not equal [%s]: %s != %s.' % (url, http_version, installed_version)
            exit(1)


def main():
    if EXEC_ENVIRONMENT != PRODUCTION_ENV:
        print 'Detected %s environment. Skipping version checks...' % EXEC_ENVIRONMENT
        exit(0)

    if GROUP == MWORKER:
        check_deploy_for_mworker()
    elif GROUP == PWORKER:
        check_deploy_for_pworker()
    elif GROUP == MPFS:
        check_deploy_for_mpfs()
    elif GROUP == API:
        check_deploy_for_api()
    elif GROUP == COPYDB:
        check_deploy_for_copydb()

if __name__ == '__main__':
    main()
