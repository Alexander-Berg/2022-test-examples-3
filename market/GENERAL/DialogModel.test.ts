import DialogModel from './DialogModel'
import Disposer from '../../shared/utils/Disposer'
import {reaction} from 'mobx'
import {Deferred} from '../../shared'

describe('DialogModel', function () {
  const disposer = new Disposer()
  const onChangeIsVisible = jest.fn()
  const onChangeParams = jest.fn()
  const onChangeIsEffectRunning = jest.fn()
  let dialogModel: DialogModel<string>

  beforeEach(() => {
    dialogModel = new DialogModel<string>()
    disposer.add(
      reaction(() => dialogModel.isVisible, onChangeIsVisible),
      reaction(() => dialogModel.isEffectRunning, onChangeIsEffectRunning),
      reaction(() => dialogModel.params, onChangeParams)
    )
  })

  afterEach(() => {
    disposer.dispose()
  })

  test('должна быть пустой при инциализации', () => {
    expect(dialogModel.isVisible).toBe(false)
  })

  test('позволяет показать диалог', () => {
    void dialogModel.open('test')

    expect(dialogModel.isVisible).toBe(true)
    expect(dialogModel.params).toBe('test')
    expect(onChangeIsVisible).toHaveBeenCalledTimes(1)
    expect(onChangeParams).toHaveBeenCalledTimes(1)
  })

  test('позволяет подтвердить действие', async () => {
    const openPromise = dialogModel.open('test')

    onChangeIsVisible.mockClear()
    onChangeParams.mockClear()

    dialogModel.confirm()

    expect(await openPromise).toBe(true)
    expect(dialogModel.isVisible).toBe(false)
    expect(onChangeIsVisible).toHaveBeenCalledTimes(1)
    expect(onChangeParams).toHaveBeenCalledTimes(0)
  })

  test('позволяет закрыть диалог', async () => {
    const openPromise = dialogModel.open('test')

    onChangeIsVisible.mockClear()
    onChangeParams.mockClear()

    dialogModel.close()

    expect(await openPromise).toBe(false)
    expect(dialogModel.isVisible).toBe(false)
    expect(onChangeIsVisible).toHaveBeenCalledTimes(1)
    expect(onChangeParams).toHaveBeenCalledTimes(0)
  })

  test('закрывает диалог при вызове dispose', async () => {
    const openPromise = dialogModel.open('test')

    dialogModel.dispose()

    expect(await openPromise).toBe(false)
  })

  test('позволяет открыть новую модалку до закрытия старой', async () => {
    const openPromise = dialogModel.open('open1')

    void dialogModel.open('open2')

    expect(await openPromise).toBe(false)
    expect(dialogModel.params).toBe('open2')
    expect(dialogModel.isVisible).toBe(true)
  })

  test('позволяет вызвать close до вызова open', () => {
    expect(() => dialogModel.close()).not.toThrowError()
  })

  test('позволяет вызвать confirm до вызова open', () => {
    expect(() => dialogModel.confirm()).not.toThrowError()
  })

  describe('при вызове open с передачей асинхроного эффекта', () => {
    describe('при вызове confirm должен вызывать effect', () => {
      test('возвращая true в случае его успешного выполнения', async () => {
        const effect = jest.fn(() => Promise.resolve('this result affects nothing'))
        const promise = dialogModel.open('open', effect)

        dialogModel.confirm()

        expect(dialogModel.isVisible).toBe(true)

        const result = await promise

        expect(result).toBe(true)
        expect(effect).toHaveBeenCalled()

        expect(dialogModel.isVisible).toBe(false)
      })

      test('возвращая false в случае ошибки', async () => {
        const effect = jest.fn(() => Promise.reject('this error will be lost'))
        const promise = dialogModel.open('open', effect)

        dialogModel.confirm()

        expect(dialogModel.isVisible).toBe(true)

        const result = await promise

        expect(result).toBe(false)
        expect(effect).toHaveBeenCalled()

        expect(dialogModel.isVisible).toBe(false)
      })
    })

    test('не должен вызывать effect при вызове close', async () => {
      const effect = jest.fn(() => Promise.resolve('this result affects nothing'))
      const promise = dialogModel.open('open', effect)

      dialogModel.close()

      expect(await promise).toBe(false)
      expect(dialogModel.isVisible).toBe(false)
      expect(effect).not.toHaveBeenCalled()
    })

    test('должен указывать на наличие запущенного эффекта', async () => {
      const defer = new Deferred()
      const effect = jest.fn(async () => await defer)
      const promise = dialogModel.open('open', effect)

      dialogModel.confirm()

      await nextTick() // Внутри open есть await, поэтому ждём когда он отработает

      expect(dialogModel.isVisible).toBe(true)
      expect(dialogModel.isEffectRunning).toBe(true)
      expect(onChangeIsEffectRunning).toHaveBeenCalled()

      onChangeIsEffectRunning.mockClear()

      defer.resolve(undefined)

      await promise

      expect(dialogModel.isVisible).toBe(false)
      expect(dialogModel.isEffectRunning).toBe(false)
      expect(onChangeIsEffectRunning).toHaveBeenCalled()
    })
  })

  test('dialogProps помогает избежать проблем с this', async () => {
    const defer = new Deferred()
    const effect = jest.fn(async () => await defer)
    const {onConfirm, onClose} = dialogModel.dialogProps
    let {visible, progress} = dialogModel.dialogProps

    disposer.add(
      reaction(
        () => dialogModel.dialogProps,
        (props) => {
          visible = props.visible
          progress = props.progress
        },
        {fireImmediately: true}
      )
    )

    expect(visible).toBe(false)
    expect(progress).toBe(false)

    void dialogModel.open('open 1')
    await nextTick()

    expect(visible).toBe(true)
    expect(progress).toBe(false)

    onClose()
    await nextTick()

    expect(visible).toBe(false)
    expect(progress).toBe(false)

    void dialogModel.open('open 2', effect)
    await nextTick()

    expect(visible).toBe(true)
    expect(progress).toBe(false)

    onConfirm()
    await nextTick()

    expect(visible).toBe(true)
    expect(progress).toBe(true)

    defer.resolve(undefined)
    await nextTick()

    expect(visible).toBe(false)
    expect(progress).toBe(false)
  })
})

function nextTick() {
  return new Promise((resolve) => process.nextTick(resolve))
}
