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
/******/ 	return __webpack_require__(__webpack_require__.s = "./app/pages.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./app/pages.js":
/*!**********************!*\
  !*** ./app/pages.js ***!
  \**********************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'app-pages',
  pages: [__webpack_require__(/*! ../pages/first */ "./pages/first/index.js"), __webpack_require__(/*! ../pages/second */ "./pages/second/index.js"), __webpack_require__(/*! ../pages/third */ "./pages/third/index.js")]
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

/***/ "./commons/images.css":
/*!****************************!*\
  !*** ./commons/images.css ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"webp":"c-commons-images__webp","jpeg":"c-commons-images__jpeg","png":"c-commons-images__png"};

/***/ }),

/***/ "./flow/flow1.js":
/*!***********************!*\
  !*** ./flow/flow1.js ***!
  \***********************/
/*! exports provided: [unsorted exports] */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "flow1", function() { return flow1; });


var flow1 = {
  module: 'flow1'
};

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
/*! exports provided: [unsorted exports] */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "module1", function() { return module1; });
/* harmony import */ var _module1_css__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./module1.css */ "./modules/module1.css");
/* harmony import */ var _module1_css__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(_module1_css__WEBPACK_IMPORTED_MODULE_0__);



var module1 = {
  module: 'module1',
  styles: _module1_css__WEBPACK_IMPORTED_MODULE_0___default.a
};

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
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "moduleConst", function() { return moduleConst; });
/* harmony import */ var _module2_css__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./module2.css */ "./modules/module2.css");
/* harmony import */ var _module2_css__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(_module2_css__WEBPACK_IMPORTED_MODULE_0__);
/* harmony reexport (unknown) */ for(var __WEBPACK_IMPORT_KEY__ in _module2_css__WEBPACK_IMPORTED_MODULE_0__) if(["default","moduleConst"].indexOf(__WEBPACK_IMPORT_KEY__) < 0) (function(key) { __webpack_require__.d(__webpack_exports__, key, function() { return _module2_css__WEBPACK_IMPORTED_MODULE_0__[key]; }) }(__WEBPACK_IMPORT_KEY__));



var moduleConst = 'module2-const';
/* harmony default export */ __webpack_exports__["default"] = ({
  module: 'module2-default'
});

/***/ }),

/***/ "./pages/first/index.css":
/*!*******************************!*\
  !*** ./pages/first/index.css ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-pages-first"};

/***/ }),

/***/ "./pages/first/index.js":
/*!******************************!*\
  !*** ./pages/first/index.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'first/page',
  // eslint-disable-next-line global-require
  widget: __webpack_require__(/*! ./widget */ "./pages/first/widget.js"),
  commons: [__webpack_require__(/*! ../../commons/comm1 */ "./commons/comm1.js")],
  vendors: [__webpack_require__(/*! ../../vendors/vend1 */ "./vendors/vend1.js")],
  styles: [__webpack_require__(/*! ./index.css */ "./pages/first/index.css")]
};

/***/ }),

/***/ "./pages/first/widget.css":
/*!********************************!*\
  !*** ./pages/first/widget.css ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-pages-first-widget"};

/***/ }),

/***/ "./pages/first/widget.js":
/*!*******************************!*\
  !*** ./pages/first/widget.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'first/widget',
  styles: [__webpack_require__(/*! ./widget.css */ "./pages/first/widget.css")]
};

/***/ }),

/***/ "./pages/second/index.css":
/*!********************************!*\
  !*** ./pages/second/index.css ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-pages-second"};

/***/ }),

/***/ "./pages/second/index.js":
/*!*******************************!*\
  !*** ./pages/second/index.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'second/page',
  // eslint-disable-next-line global-require
  widget: __webpack_require__(/*! ./widget */ "./pages/second/widget.js"),
  styles: [__webpack_require__(/*! ./index.css */ "./pages/second/index.css")]
};

/***/ }),

/***/ "./pages/second/widget.css":
/*!*********************************!*\
  !*** ./pages/second/widget.css ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-pages-second-widget"};

/***/ }),

/***/ "./pages/second/widget.js":
/*!********************************!*\
  !*** ./pages/second/widget.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'second/widget',
  commons: [__webpack_require__(/*! ../../commons/comm2 */ "./commons/comm2.js")],
  vendors: [__webpack_require__(/*! ../../vendors/vend2 */ "./vendors/vend2.js")],
  styles: [__webpack_require__(/*! ./widget.css */ "./pages/second/widget.css")]
};

/***/ }),

/***/ "./pages/third/index.css":
/*!*******************************!*\
  !*** ./pages/third/index.css ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-pages-third"};

/***/ }),

/***/ "./pages/third/index.js":
/*!******************************!*\
  !*** ./pages/third/index.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'third/page',
  // eslint-disable-next-line global-require
  widget: __webpack_require__(/*! ./widget */ "./pages/third/widget.js"),
  styles: [__webpack_require__(/*! ./index.css */ "./pages/third/index.css")]
};

/***/ }),

/***/ "./pages/third/widget.css":
/*!********************************!*\
  !*** ./pages/third/widget.css ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

// extracted by mini-css-extract-plugin
module.exports = {"root":"c-pages-third-widget"};

/***/ }),

/***/ "./pages/third/widget.js":
/*!*******************************!*\
  !*** ./pages/third/widget.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


module.exports = {
  module: 'third/widget',
  styles: [__webpack_require__(/*! ./widget.css */ "./pages/third/widget.css")]
};

/***/ }),

/***/ "./resolvers/async.js":
/*!****************************!*\
  !*** ./resolvers/async.js ***!
  \****************************/
/*! exports provided: [unsorted exports] */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "asyncResolver", function() { return asyncResolver; });
/* harmony import */ var _yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @yandex-market/mandrel/resolver */ "@yandex-market/mandrel/resolver");
/* harmony import */ var _yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(_yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__);


// eslint-disable-next-line import/extensions,import/no-unresolved

var asyncResolver = Object(_yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__["createResolver"])(function (ctx) {
  return Promise.resolve({
    asyncResolver: true,
    ctx: ctx
  });
}, {
  name: 'asyncResolver'
});

/***/ }),

/***/ "./resolvers/remote.js":
/*!*****************************!*\
  !*** ./resolvers/remote.js ***!
  \*****************************/
/*! exports provided: [unsorted exports] */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "remoteResolver", function() { return remoteResolver; });
/* harmony import */ var _yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @yandex-market/mandrel/resolver */ "@yandex-market/mandrel/resolver");
/* harmony import */ var _yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(_yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__);


// eslint-disable-next-line import/extensions,import/no-unresolved

var remoteResolver = Object(_yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__["createResolver"])(function (ctx) {
  return Promise.resolve({
    remoteResolver: true,
    ctx: ctx
  });
}, {
  name: 'remoteResolver',
  remote: true
});

/***/ }),

/***/ "./resolvers/sync.js":
/*!***************************!*\
  !*** ./resolvers/sync.js ***!
  \***************************/
/*! exports provided: [unsorted exports] */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "syncResolver", function() { return syncResolver; });
/* harmony import */ var _yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @yandex-market/mandrel/resolver */ "@yandex-market/mandrel/resolver");
/* harmony import */ var _yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(_yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__);


// eslint-disable-next-line import/extensions,import/no-unresolved

var syncResolver = Object(_yandex_market_mandrel_resolver__WEBPACK_IMPORTED_MODULE_0__["createSyncResolver"])(function (ctx) {
  return {
    syncResolver: true,
    ctx: ctx
  };
}, {
  name: 'syncResolver'
});

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