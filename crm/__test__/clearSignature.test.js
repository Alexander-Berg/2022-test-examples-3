/* global window, describe, it, expect, test*/
import jquery from 'jquery';
import clearSignature from '../clearSignature';

window.$ = jquery;

const oneDiv = {
  in: '<div class="spSignature"></div>',
  out: '<div class=""></div>',
};

const oneDivtwoClass = {
  in: '<div class="spSignature someclass"></div>',
  out: '<div class="someclass"></div>',
};

const twoDiv = {
  in:
    '<div><div class="spSignature someclass"></div><div class="spSignature someclass"></div></div>',
  out: '<div><div class="someclass"></div><div class="someclass"></div></div>',
};

const realWorld = {
  in:
    '<div>&nbsp;</div><div>-------- Пересылаемое сообщение--------</div><div>12.04.2017, 10:31, "Яндекс. Клиентский сервис" &lt;client-service@yandex-team.com.ua&gt;:</div><p>&nbsp;</p><div>&nbsp;<div>&nbsp;<div class="spSignature"><div><div>-</div><div>Please rate the response you received https://yandex.com/support/survey/?sid=c587ed03</div><div>-<br>Customer Service Department<br>tel.: 8 800 234-24-80 (free calls from anywhere in Russia)<br>tel.: +7 495 739-37-77<br>https://yandex.com/support/direct/?from=email</div></div></div>&nbsp;<div>&nbsp;</div><div>-------- Пересылаемое сообщение--------</div><div>13.03.2017, 17:00, "TestUnknown" &lt;agroroza@yandex-team.ru&gt;:</div><div>&nbsp;</div><div>Direct Eng test message.</div><div>&nbsp;</div><div>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div><div>&nbsp;</div></div></div><p>&nbsp;</p>',
  out:
    '<div>&nbsp;</div><div>-------- Пересылаемое сообщение--------</div><div>12.04.2017, 10:31, "Яндекс. Клиентский сервис" &lt;client-service@yandex-team.com.ua&gt;:</div><p>&nbsp;</p><div>&nbsp;<div>&nbsp;<div class=""><div><div>-</div><div>Please rate the response you received https://yandex.com/support/survey/?sid=c587ed03</div><div>-<br>Customer Service Department<br>tel.: 8 800 234-24-80 (free calls from anywhere in Russia)<br>tel.: +7 495 739-37-77<br>https://yandex.com/support/direct/?from=email</div></div></div>&nbsp;<div>&nbsp;</div><div>-------- Пересылаемое сообщение--------</div><div>13.03.2017, 17:00, "TestUnknown" &lt;agroroza@yandex-team.ru&gt;:</div><div>&nbsp;</div><div>Direct Eng test message.</div><div>&nbsp;</div><div>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div><div>&nbsp;</div></div></div><p>&nbsp;</p>',
};

const noMatch = '<div><div class="someclass"></div><div class="someclass"></div></div>';

describe('clear signature', () => {
  it('should be undefiend', () => {
    expect(clearSignature()).toBe(undefined);
  });

  it('should be empty string', () => {
    expect(clearSignature('')).toBe('');
  });

  test('no modifed', () => {
    expect(clearSignature(noMatch)).toBe(noMatch);
  });

  test('one div', () => {
    expect(clearSignature(oneDiv.in)).toBe(oneDiv.out);
  });

  test('one div, two classes', () => {
    expect(clearSignature(oneDivtwoClass.in)).toBe(oneDivtwoClass.out);
  });

  test('two div', () => {
    expect(clearSignature(twoDiv.in)).toBe(twoDiv.out);
  });

  test('real world test', () => {
    expect(clearSignature(realWorld.in)).toBe(realWorld.out);
  });
});
