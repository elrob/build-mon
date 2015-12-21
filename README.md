[![Build Status](https://snap-ci.com/elrob/build-mon/branch/master/build_image)](https://snap-ci.com/elrob/build-mon/branch/master)

# build-mon

A simple build monitor to monitor Visual Studio Online builds

## Usage

Install [Leiningen](http://leiningen.org/)

Obtain a personal access token for Visual Studio Online with two permissions boxes ticked:
'Build (read)' and 'Code (read)'

Run app:

    lein run "VSO_ACCOUNT_NAME" "VSO_PROJECT_NAME" "VSO_PERSONAL_ACCESS_TOKEN"


Visit [localhost:3000](http://localhost:3000)

## Development

Run tests:

    lein midje [:autotest]

