from git import Repo
import os

Repo.clone_from('https://github.yandex-team.ru/a-zoshchuk/accessorMailTool','./repoDir')
os.system("cloc --yaml repoDir > lines.yaml")
