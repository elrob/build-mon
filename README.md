# build-mon

[![Build Status](https://snap-ci.com/elrob/build-mon/branch/master/build_image)](https://snap-ci.com/elrob/build-mon/branch/master)

A simple build monitor to monitor Visual Studio Online builds

## Usage

Install [Leiningen](http://leiningen.org/)

Obtain a personal access token for Visual Studio Online with two permissions boxes ticked:

- **Build (read)** for dispaying build status
- **Code (read)**  for displaying commit messages (optional)

Run app:

    lein run "VSO_ACCOUNT_NAME" "VSO_PROJECT_NAME" "VSO_PERSONAL_ACCESS_TOKEN"


Visit [localhost:3000](http://localhost:3000)

Deploy app to heroku:

- To deploy the app to Heroku you need to supply Heroku with a port number. Do this through changing line 114 in build_mon/core.clj to the following

```
(let [port (Integer. (or (or (System/getenv "PORT") port) 3000))]
```


## Development

Run tests:

    lein midje [:autotest]
