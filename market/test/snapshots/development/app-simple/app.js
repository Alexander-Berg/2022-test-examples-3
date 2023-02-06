module.exports =
/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, { enumerable: true, get: getter });
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 			Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 		}
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// create a fake namespace object
/******/ 	// mode & 1: value is a module id, require it
/******/ 	// mode & 2: merge all properties of value into the ns
/******/ 	// mode & 4: return value when already ns object
/******/ 	// mode & 8|1: behave like require
/******/ 	__webpack_require__.t = function(value, mode) {
/******/ 		if(mode & 1) value = __webpack_require__(value);
/******/ 		if(mode & 8) return value;
/******/ 		if((mode & 4) && typeof value === 'object' && value && value.__esModule) return value;
/******/ 		var ns = Object.create(null);
/******/ 		__webpack_require__.r(ns);
/******/ 		Object.defineProperty(ns, 'default', { enumerable: true, value: value });
/******/ 		if(mode & 2 && typeof value != 'string') for(var key in value) __webpack_require__.d(ns, key, function(key) { return value[key]; }.bind(null, key));
/******/ 		return ns;
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = "./app/simple.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./app/simple.js":
/*!***********************!*\
  !*** ./app/simple.js ***!
  \***********************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'app-simple',
  pages: [__webpack_require__(/*! ../first */ "./first.js"), __webpack_require__(/*! ../second */ "./second.js"), __webpack_require__(/*! ../third */ "./third.js")]
};

/***/ }),

/***/ "./commons/comm1.css":
/*!***************************!*\
  !*** ./commons/comm1.css ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-commons-comm-1"};

/***/ }),

/***/ "./commons/comm1.js":
/*!**************************!*\
  !*** ./commons/comm1.js ***!
  \**************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'comm1',
  styles: [__webpack_require__(/*! ./comm1.css */ "./commons/comm1.css"), __webpack_require__(/*! ./images.css */ "./commons/images.css")],
  modules: [__webpack_require__(/*! ../modules/module1 */ "./modules/module1.js")],
  flow: [__webpack_require__(/*! ../flow/flow1 */ "./flow/flow1.js")],
  resolvers: [__webpack_require__(/*! ../resolvers/sync */ "./resolvers/sync.js"), __webpack_require__(/*! ../resolvers/async */ "./resolvers/async.js"), __webpack_require__(/*! ../resolvers/remote */ "./resolvers/remote.js")],
  externals: [__webpack_require__(/*! react */ "react")]
};

/***/ }),

/***/ "./commons/comm2.css":
/*!***************************!*\
  !*** ./commons/comm2.css ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-commons-comm-2"};

/***/ }),

/***/ "./commons/comm2.js":
/*!**************************!*\
  !*** ./commons/comm2.js ***!
  \**************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'comm2',
  styles: [__webpack_require__(/*! ./comm2.css */ "./commons/comm2.css")],
  modules: [__webpack_require__(/*! ../modules/module2 */ "./modules/module2.js")]
};

/***/ }),

/***/ "./commons/comm3.css":
/*!***************************!*\
  !*** ./commons/comm3.css ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-commons-comm-3"};

/***/ }),

/***/ "./commons/comm3.js":
/*!**************************!*\
  !*** ./commons/comm3.js ***!
  \**************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'comm3',
  styles: [__webpack_require__(/*! ./comm3.css */ "./commons/comm3.css")],
  modules: [__webpack_require__(/*! ../modules/module3 */ "./modules/module3.js")]
};

/***/ }),

/***/ "./commons/images.css":
/*!****************************!*\
  !*** ./commons/images.css ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"webp":"c-commons-images__webp","jpeg":"c-commons-images__jpeg","png":"c-commons-images__png"};

/***/ }),

/***/ "./first.css":
/*!*******************!*\
  !*** ./first.css ***!
  \*******************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-first"};

/***/ }),

/***/ "./first.js":
/*!******************!*\
  !*** ./first.js ***!
  \******************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'first',
  commons: [__webpack_require__(/*! ./commons/comm1 */ "./commons/comm1.js")],
  vendors: [__webpack_require__(/*! ./vendors/vend1 */ "./vendors/vend1.js")],
  styles: [__webpack_require__(/*! ./first.css */ "./first.css")]
};

/***/ }),

/***/ "./flow/flow1.js":
/*!***********************!*\
  !*** ./flow/flow1.js ***!
  \***********************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.flow1 = void 0;
const flow1 = {
  module: 'flow1'
};
exports.flow1 = flow1;

/***/ }),

/***/ "./modules/module1.css":
/*!*****************************!*\
  !*** ./modules/module1.css ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-modules-module-1"};

/***/ }),

/***/ "./modules/module1.js":
/*!****************************!*\
  !*** ./modules/module1.js ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.module1 = void 0;

var _module = _interopRequireDefault(__webpack_require__(/*! ./module1.css */ "./modules/module1.css"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const module1 = {
  module: 'module1',
  styles: _module.default
};
exports.module1 = module1;

/***/ }),

/***/ "./modules/module2.css":
/*!*****************************!*\
  !*** ./modules/module2.css ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-modules-module-2"};

/***/ }),

/***/ "./modules/module2.js":
/*!****************************!*\
  !*** ./modules/module2.js ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
var _exportNames = {
  moduleConst: true
};
exports.moduleConst = exports.default = void 0;

var _module = __webpack_require__(/*! ./module2.css */ "./modules/module2.css");

Object.keys(_module).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (Object.prototype.hasOwnProperty.call(_exportNames, key)) return;
  if (key in exports && exports[key] === _module[key]) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function () {
      return _module[key];
    }
  });
});
const moduleConst = 'module2-const';
exports.moduleConst = moduleConst;
var _default = {
  module: 'module2-default'
};
exports.default = _default;

/***/ }),

/***/ "./modules/module3.css":
/*!*****************************!*\
  !*** ./modules/module3.css ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-modules-module-3"};

/***/ }),

/***/ "./modules/module3.js":
/*!****************************!*\
  !*** ./modules/module3.js ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
var _exportNames = {};
exports.default = void 0;

var _module = _interopRequireDefault(__webpack_require__(/*! ./module3.css */ "./modules/module3.css"));

var _module2 = _interopRequireWildcard(__webpack_require__(/*! ./module2 */ "./modules/module2.js"));

var _module3 = __webpack_require__(/*! ./module1 */ "./modules/module1.js");

Object.keys(_module3).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (Object.prototype.hasOwnProperty.call(_exportNames, key)) return;
  if (key in exports && exports[key] === _module3[key]) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function () {
      return _module3[key];
    }
  });
});

function _getRequireWildcardCache(nodeInterop) { if (typeof WeakMap !== "function") return null; var cacheBabelInterop = new WeakMap(); var cacheNodeInterop = new WeakMap(); return (_getRequireWildcardCache = function (nodeInterop) { return nodeInterop ? cacheNodeInterop : cacheBabelInterop; })(nodeInterop); }

function _interopRequireWildcard(obj, nodeInterop) { if (!nodeInterop && obj && obj.__esModule) { return obj; } if (obj === null || typeof obj !== "object" && typeof obj !== "function") { return { default: obj }; } var cache = _getRequireWildcardCache(nodeInterop); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (key !== "default" && Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } newObj.default = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var _default = {
  module: 'module3',
  module2: [_module2.default, _module2.moduleConst],
  styles: _module.default
};
exports.default = _default;

/***/ }),

/***/ "./resolvers/async.js":
/*!****************************!*\
  !*** ./resolvers/async.js ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.asyncResolver = void 0;

var _resolver = __webpack_require__(/*! @yandex-market/mandrel/resolver */ "@yandex-market/mandrel/resolver");

// eslint-disable-next-line import/extensions,import/no-unresolved
const asyncResolver = (0, _resolver.createResolver)(ctx => Promise.resolve({
  asyncResolver: true,
  ctx
}), {
  name: 'asyncResolver'
});
exports.asyncResolver = asyncResolver;

/***/ }),

/***/ "./resolvers/remote.js":
/*!*****************************!*\
  !*** ./resolvers/remote.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.remoteResolver = void 0;

var _resolver = __webpack_require__(/*! @yandex-market/mandrel/resolver */ "@yandex-market/mandrel/resolver");

// eslint-disable-next-line import/extensions,import/no-unresolved
const remoteResolver = (0, _resolver.createResolver)(ctx => Promise.resolve({
  remoteResolver: true,
  ctx
}), {
  name: 'remoteResolver',
  remote: true
});
exports.remoteResolver = remoteResolver;

/***/ }),

/***/ "./resolvers/sync.js":
/*!***************************!*\
  !*** ./resolvers/sync.js ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.syncResolver = void 0;

var _resolver = __webpack_require__(/*! @yandex-market/mandrel/resolver */ "@yandex-market/mandrel/resolver");

// eslint-disable-next-line import/extensions,import/no-unresolved
const syncResolver = (0, _resolver.createSyncResolver)(ctx => ({
  syncResolver: true,
  ctx
}), {
  name: 'syncResolver'
});
exports.syncResolver = syncResolver;

/***/ }),

/***/ "./second.css":
/*!********************!*\
  !*** ./second.css ***!
  \********************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-second"};

/***/ }),

/***/ "./second.js":
/*!*******************!*\
  !*** ./second.js ***!
  \*******************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'second',
  commons: [__webpack_require__(/*! ./commons/comm1 */ "./commons/comm1.js"), __webpack_require__(/*! ./commons/comm2 */ "./commons/comm2.js")],
  vendors: [__webpack_require__(/*! ./vendors/vend1 */ "./vendors/vend1.js"), __webpack_require__(/*! ./vendors/vend2 */ "./vendors/vend2.js")],
  styles: [__webpack_require__(/*! ./second.css */ "./second.css")]
};

/***/ }),

/***/ "./third.css":
/*!*******************!*\
  !*** ./third.css ***!
  \*******************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-third"};

/***/ }),

/***/ "./third.js":
/*!******************!*\
  !*** ./third.js ***!
  \******************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'third',
  commons: [__webpack_require__(/*! ./commons/comm1 */ "./commons/comm1.js"), __webpack_require__(/*! ./commons/comm2 */ "./commons/comm2.js"), __webpack_require__(/*! ./commons/comm3 */ "./commons/comm3.js")],
  vendors: [__webpack_require__(/*! ./vendors/vend1 */ "./vendors/vend1.js"), __webpack_require__(/*! ./vendors/vend2 */ "./vendors/vend2.js"), __webpack_require__(/*! ./vendors/vend3 */ "./vendors/vend3.js")],
  styles: [__webpack_require__(/*! ./third.css */ "./third.css")]
};

/***/ }),

/***/ "./vendors/vend1.css":
/*!***************************!*\
  !*** ./vendors/vend1.css ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-vendors-vend-1"};

/***/ }),

/***/ "./vendors/vend1.js":
/*!**************************!*\
  !*** ./vendors/vend1.js ***!
  \**************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'vend1',
  styles: [__webpack_require__(/*! ./vend1.css */ "./vendors/vend1.css")]
};

/***/ }),

/***/ "./vendors/vend2.css":
/*!***************************!*\
  !*** ./vendors/vend2.css ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-vendors-vend-2"};

/***/ }),

/***/ "./vendors/vend2.js":
/*!**************************!*\
  !*** ./vendors/vend2.js ***!
  \**************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'vend2',
  styles: [__webpack_require__(/*! ./vend2.css */ "./vendors/vend2.css")]
};

/***/ }),

/***/ "./vendors/vend3.css":
/*!***************************!*\
  !*** ./vendors/vend3.css ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-vendors-vend-3"};

/***/ }),

/***/ "./vendors/vend3.js":
/*!**************************!*\
  !*** ./vendors/vend3.js ***!
  \**************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'vend3',
  styles: [__webpack_require__(/*! ./vend3.css */ "./vendors/vend3.css")]
};

/***/ }),

/***/ "@yandex-market/mandrel/resolver":
/*!**************************************************!*\
  !*** external "@yandex-market/mandrel/resolver" ***!
  \**************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = require("@yandex-market/mandrel/resolver");

/***/ }),

/***/ "react":
/*!************************!*\
  !*** external "react" ***!
  \************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = require("react");

/***/ })

/******/ });
//# sourceMappingURL=[source map goes here]