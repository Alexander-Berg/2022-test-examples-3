import fs from 'fs'
import path from 'path'
import * as fileCache from '@yandex-int/file-cacher'
import {decode, encode} from 'querystring'
import * as crypto from 'crypto'

interface ParsedRequestDump {
  meta: {
    key: string
    url: string
    method: string
    sourceRes: {
      host: string
      statusCode: number
      url: string
      headers: Record<string, string>
      timeTaken: number
    }
    separateData: boolean
    body: string
  }
}

const cacheReader = new fileCache.Reader()
const cacheWriter = new fileCache.Writer()

function walk(rootPath: string, callback: (content: string, filePath: string) => void) {
  const dir = fs.readdirSync(rootPath)

  dir.forEach((fileName: string) => {
    const filePath = rootPath + '/' + fileName

    const stats = fs.statSync(filePath)
    if (stats.isDirectory()) {
      walk(filePath, callback)
    } else if (filePath.includes('json.gz')) {
      cacheReader
        .get(filePath)
        .then((content) => callback(content.toString(), filePath))
        .catch((err) => {
          console.error('Cache reader error: ', err)
        })
    }
  })
}

interface Options {
  test?: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  url: RegExp
  requestBodyProcessor(oldBody?: unknown): unknown
  sourceResProcessor(sourceRes: ParsedRequestDump['meta']['sourceRes']): ParsedRequestDump['meta']['sourceRes']
}

function patchDumps({url, method, test = '', requestBodyProcessor, sourceResProcessor}: Options) {
  const rootPath = path.resolve(__dirname, 'suites/common', test)

  walk(rootPath, (content, filePath) => {
    const {meta}: ParsedRequestDump = JSON.parse(content)

    if (url.test(meta.url) && meta.method === method) {
      meta.sourceRes = sourceResProcessor(meta.sourceRes)
      const newBody = requestBodyProcessor(meta.body && JSON.parse(meta.body))
      meta.body = JSON.stringify(newBody)
      // Обновляем ключ хеша боди
      const decodedKey = decode(meta.key)
      decodedKey.body = crypto.createHash('sha256').update(meta.body).digest('hex')
      meta.key = encode(decodedKey)

      // Обновляем ключ хеша файла с дампом
      const prevCacheKeyHash = path.parse(filePath).name.split('.')[2]
      const newCacheKeyHash = crypto.createHash('sha256').update(meta.key).digest('hex')

      void cacheWriter.set(filePath.replace(prevCacheKeyHash, newCacheKeyHash), Buffer.from(JSON.stringify({meta})))

      // Переименовываем response
      const oldResponseFilePath = filePath.replace('json', 'data')
      const newResponseFilePath = oldResponseFilePath.replace(prevCacheKeyHash, newCacheKeyHash)
      fs.renameSync(oldResponseFilePath, newResponseFilePath)

      fs.unlinkSync(filePath)
    }
  })
}

patchDumps({
  method: 'GET',
  test: 'webvendor-168',
  url: /\/4.0\/restapp-front\/place\/v1\/autostop-rules/g,
  requestBodyProcessor(oldBody: unknown) {
    // Update request body here
    return oldBody
  },
  sourceResProcessor(sourceRes: ParsedRequestDump['meta']['sourceRes']) {
    return sourceRes
  }
})
