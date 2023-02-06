import fs from 'fs'
import path from 'path'
import groupBy from 'lodash/groupBy'

type TestIdAttribute = {
  value: string
  group: string
  description: string
  source: string
}

const BITBUCKET_REPO_URL = 'https://bb.yandex-team.ru/projects/EDA/repos/web_vendor/browse'
const REPOSITORY_ROOT = path.resolve(__dirname, '..')
const ATTRIBUTE_NAME = 'data-testid'
const DOCS_FILENAME = 'TEST-SELECTORS.md'
const GENERATED_SELECTORS_DIRECTORY = REPOSITORY_ROOT + '/tests/page-objects/selectors.generated'

const attributeValueRegex = /(?<={\s*['`]).*?(?=['`])/
const commentRegex = /(?<=\/\*)(.|\n)*(?=\*\/)/

const objectPropertyRegex = new RegExp(`'${ATTRIBUTE_NAME}':\\s*['\`].*['\`][\\s]+\\/\\*.*\\*\\/`, 'g')
const jsxPropertyRegex = new RegExp(`${ATTRIBUTE_NAME}={\\s*(.|)*?\\*\\/\\s*}`, 'g')

function objectNotationToJsxNotation(objectForm: string) {
  // 'data-testid': 'auth__password' /*Auth | Поле ввода пароля*/
  // to
  // data-testid={'auth__password' /*Auth | Поле ввода пароля*/}
  return objectForm.replace(new RegExp(`'${ATTRIBUTE_NAME}':\\s*`), `${ATTRIBUTE_NAME}={`) + '}'
}

function extractAttributes(code: string) {
  const jsxAttributes = code.match(jsxPropertyRegex) || []
  const objectAttributes = code.match(objectPropertyRegex) || []

  return jsxAttributes.concat(objectAttributes.map(objectNotationToJsxNotation))
}

function walk(rootPath: string, callback: (code: string, filePath: string) => void) {
  const dir = fs.readdirSync(rootPath)
  dir.forEach((fileName: string) => {
    const filePath = rootPath + '/' + fileName

    const stats = fs.statSync(filePath)
    if (stats.isDirectory()) {
      walk(filePath, callback)
    } else if (path.extname(filePath) === '.tsx') {
      const code = fs.readFileSync(filePath, {encoding: 'utf8'})
      callback(code, filePath)
    }
  })
}

function prettifyInterpolation(str: string) {
  const interpolations = /(?<=\${).+(?=})/.exec(str)

  if (interpolations) {
    const removeDots = (s: string) =>
      s
        .split('.')
        .map((pathPart, i) => (i === 0 ? pathPart : pathPart[0].toUpperCase() + pathPart.slice(1)))
        .join('')

    str = interpolations.reduce((result, path) => result.replace(path, removeDots(path)), str)
  }

  return str.replace('$', '')
}

function parseAttribute(attribute: string) {
  const value = attributeValueRegex.exec(attribute)
  if (!value) {
    throw new Error('Cannot parse attribute value')
  }

  const comment = commentRegex.exec(attribute)
  if (!comment) {
    throw new Error('Undocumented selector')
  }

  const [group, description] = comment[0].split('|').map((c) => c.trim())

  return {value: prettifyInterpolation(value[0]), group, description}
}

function snakeToCamel(str: string) {
  return str.replace(/([-_][a-z0-9])/g, (group) => group.toUpperCase().replace('-', '').replace('_', ''))
}

function formatSourcePath(filePath: string) {
  const relativePath = path.relative(REPOSITORY_ROOT, filePath)

  return BITBUCKET_REPO_URL + '/' + relativePath
}

function formatGroup(name: string) {
  return `## ${name}\n`
}

function formatRow(attribute: TestIdAttribute) {
  const fileName = attribute.source.substr(attribute.source.lastIndexOf('/') + 1)
  return `\`${ATTRIBUTE_NAME}="${attribute.value}"\` | ${attribute.description} | [${fileName}](${attribute.source})\n`
}

const MARKDOWN_START = `[//]: # (This docs generates automatically. To update docs use npm run build-test-docs)
# Список доступных селекторов

Если часть селектора записана в фигурных скобках, значит она динамическая. 

data-testid="item-{id}" => data-testid="item-5"
`

const TABLE = `
Селектор | Описание | Source
--- | --- | ---
`

function buildSelectors(testIdAttributes: TestIdAttribute[]) {
  const groupedAttrs = groupBy(testIdAttributes, (attr: TestIdAttribute) => attr.value.split('__')[0])

  function attrToTs(attr: TestIdAttribute): string {
    let getter: string
    const dynamicParams = /(?<={).+(?=})/.exec(attr.value)
    if (dynamicParams) {
      const getterArgs = dynamicParams.map((paramName) => `${paramName}: string | number`).join()

      const value = dynamicParams.reduce((result, param) => {
        const dynamicParamRegex = new RegExp(`{${param}}`)
        return result.replace(dynamicParamRegex, `\${${param}}`)
      }, attr.value)

      const getterName = dynamicParams
        .reduce((name, param) => {
          const dynamicParamRegex = new RegExp(`-?{${param}}`)
          return name.replace(dynamicParamRegex, '')
        }, attr.value)
        .split('__')[1]

      getter = `static ${snakeToCamel(getterName)}(${getterArgs}) { return \`[data-testid="${value}"]\`}`
    } else {
      getter = `static ${snakeToCamel(attr.value.split('__')[1])} = '[data-testid="${attr.value}"]'`
    }

    return `// ${attr.description}
    ${getter}
    `
  }

  fs.mkdirSync(GENERATED_SELECTORS_DIRECTORY, {recursive: true})

  Object.keys(groupedAttrs).forEach((groupName) => {
    const hasDynamicParams = (s: string) => Number(!!/{.+}/.exec(s))
    // const propertiesFirst = (a, b) => hasDynamicParams(a.value) - hasDynamicParams(b.value)
    const className = groupName[0].toUpperCase() + groupName.slice(1) + 'Selectors'

    const SelectorsClass = `export class ${className} {
      ${groupedAttrs[groupName]
        .sort((a, b) => hasDynamicParams(a.value) - hasDynamicParams(b.value))
        .map(attrToTs)
        .join('')}
    }`

    fs.writeFile(`${GENERATED_SELECTORS_DIRECTORY}/${className}.ts`, SelectorsClass, (err) => err && console.error(err))
  })
}

function buildTestDocs(attributes: TestIdAttribute[]) {
  const groupedAttrs = groupBy(attributes, (attr: TestIdAttribute) => attr.group)

  let markdown = MARKDOWN_START

  Object.keys(groupedAttrs)
    .sort()
    .forEach((group) => {
      markdown += formatGroup(group)
      markdown += TABLE
      groupedAttrs[group].forEach((attr: TestIdAttribute) => {
        markdown += formatRow(attr)
      })
    })

  const filePath = `${REPOSITORY_ROOT}/tests/${DOCS_FILENAME}`
  fs.writeFileSync(filePath, markdown)
}

function getTestIdAttributes(): TestIdAttribute[] {
  const attributesMap = new Map<string, TestIdAttribute>()

  walk(REPOSITORY_ROOT + '/src', (code, filePath) => {
    extractAttributes(code).forEach((attr) => {
      const parsedAttr = parseAttribute(attr)

      if (attributesMap.has(parsedAttr.value)) {
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        const {group, description} = attributesMap.get(parsedAttr.value)!

        if (group !== parsedAttr.group || description !== parsedAttr.description) {
          throw new Error(`Duplicate attribute value('${parsedAttr.value}') with different description. 
          Attribute values must be unique between different elements.
          If this value is need to be used for several elements of the same functionality, then the description must be 
          identical. Or, you can provide description only in one place and provide rest of the attributes without 
          description, like this: data-testid='value'`)
        }

        return
      }

      attributesMap.set(parsedAttr.value, {...parsedAttr, source: formatSourcePath(filePath)})
    })
  })

  return Array.from(attributesMap.values())
}

const testAttrs = getTestIdAttributes()

buildTestDocs(testAttrs)
buildSelectors(testAttrs)
