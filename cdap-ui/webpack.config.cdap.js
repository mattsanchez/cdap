/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var webpack = require('webpack');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var path = require('path');
var LiveReloadPlugin = require('webpack-livereload-plugin');
var LodashModuleReplacementPlugin = require('lodash-webpack-plugin');
var autoprefixer = require('autoprefixer');
var StyleLintPlugin = require('stylelint-webpack-plugin');

var plugins = [
  new webpack.optimize.CommonsChunkPlugin("common", "common.js", Infinity),
  new LodashModuleReplacementPlugin,
  new LiveReloadPlugin(),
  new webpack.optimize.DedupePlugin(),
  new CopyWebpackPlugin([
    {
      from: './cdap.html',
      to: './cdap.html'
    },
    {
      from: './styles/fonts',
      to: './fonts/'
    },
    {
      from: path.resolve(__dirname, 'node_modules', 'font-awesome', 'fonts'),
      to: './fonts/'
    },
    {
      from: './styles/img',
      to: './img/'
    }
  ]),
  new StyleLintPlugin({
    syntax: 'scss',
    files: ['**/*.scss']
  })
];
var mode = process.env.NODE_ENV;

if (mode === 'production' || mode === 'build') {
  plugins.push(
    new webpack.DefinePlugin({
      'process.env':{
        'NODE_ENV': JSON.stringify("production"),
        '__DEVTOOLS__': false
      },
    }),
    new webpack.optimize.UglifyJsPlugin({
      compress: {
          warnings: false
      }
    })
  );
}

var loaders = [
  {
    test: /\.scss$/,
    loader: 'style-loader!css-loader!postcss-loader!sass-loader'
  },
  {
    test: /\.ya?ml$/,
    loader: 'yml'
  },
  {
    test: /\.css$/,
    loader: 'style-loader!css-loader!sass-loader'
  },
  {
    test: /\.js$/,
    loader: 'babel',
    exclude: [
      /node_modules/,
      /lib/
    ],
    query: {
      plugins: ['lodash'],
      presets: ['react', 'es2015']
    }
  },
  {
    test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
    loader: 'url-loader?limit=10000&mimetype=application/font-woff'
  },
  {
    test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
    loader: 'file-loader'
  }
];

module.exports = {
  context: __dirname + '/app/cdap',
  entry: {
    'cdap': ['./cdap.js'],
    'common': [
      'whatwg-fetch',
      'react',
      'react-dom',
      'redux',
      'lodash',
      'classnames',
      'node-uuid',
      'sockjs-client',
      'rx',
      'reactstrap',
      'react-addons-css-transition-group',
      'i18n-react',
      'fuse.js',
      'react-dropzone',
      'react-redux',
      'react-router',
      'moment',
      'react-file-download',
      'mousetrap',
      'papaparse',
      'rx-dom',
      'd3',
      'chart.js',
      'cdap-avsc'
    ]
  },
  module: {
    preLoaders: [
      {
        test: /\.js$/,
        loader: 'eslint-loader',
        exclude: [
          /node_modules/,
          /bower_components/,
          /dist/,
          /old_dist/,
          /cdap_dist/,
          /common_dist/,
          /lib/,
          /wrangler_dist/
        ]
      }
    ],
    loaders: loaders
  },
  postcss: [
    autoprefixer({ browsers: ['> 1%'], cascade:true })
  ],
  output: {
    filename: './[name].js',
    path: __dirname + '/cdap_dist/cdap_assets'
  },
  plugins: plugins,
  resolve: {
    alias: {
      components: __dirname + '/app/cdap/components',
      services: __dirname + '/app/cdap/services',
      api: __dirname + '/app/cdap/api',
      lib: __dirname + '/app/lib'
    }
  }
};
