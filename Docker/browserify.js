// JS file used to buiild a standalone version of timidity.
// Use with command "browserify browserify.js --standalone timidity -o timidity.js" .

const Timidity = require('timidity')

module.exports = function getPlayer(wasmUrl) {
    return new Timidity(wasmUrl);
  }