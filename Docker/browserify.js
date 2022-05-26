// JS file used to build a standalone version of timidity.
// Use with command "browserify browserify.js --standalone timidity -o timidity.js" .
// While it's in the Docker folder, it's not actually used in any way since there's already a prebuilt timidity.js file in WebContent..

const Timidity = require('timidity')

module.exports = function getPlayer(wasmUrl) {
    return new Timidity(wasmUrl);
  }