#!/bin/sh

echo "Installing front-end dependencies..."

mkdir WebContent/js/vendor
mkdir WebContent/css/vendor

cp node_modules/jquery/dist/jquery.min.js WebContent/js/vendor
cp node_modules/jquery-ui/dist/jquery-ui.min.js WebContent/js/vendor
cp node_modules/blueimp-file-upload/js/jquery.fileupload.js WebContent/js/vendor
cp node_modules/@materializecss/materialize/dist/js/materialize.min.js WebContent/js/vendor
cp -r node_modules/wahdio/ WebContent/js/vendor

cp node_modules/@materializecss/materialize/dist/css/materialize.min.css WebContent/css/vendor
cp node_modules/blueimp-file-upload/css/jquery.fileupload.css WebContent/css/vendor
cp node_modules/animate.css/animate.min.css WebContent/css/vendor

npm install -g browserify

browserify -r mio-player -o WebContent/js/vendor/mio-player.js -s mio
browserify -r mio-midi -o WebContent/js/vendor/mio-midi.js -s mio_midi