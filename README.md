# build-mon

[![Build Status](https://snap-ci.com/elrob/build-mon/branch/master/build_image)](https://snap-ci.com/elrob/build-mon/branch/master)

A simple project monitor to display the status of Visual Studio Online builds and releases.

![Screenshot](screenshot.png)

## Usage

Install [Leiningen](http://leiningen.org/)

Obtain a personal access token for Visual Studio Online with the following permissions boxes ticked:

- **Build (read)** for displaying status of builds
- **Release (read)** for displaying status of releases
- **Code (read)**  for displaying commit messages (optional)

Run app:

    lein run "VSO_ACCOUNT_NAME" "VSO_PROJECT_NAME" "VSO_PERSONAL_ACCESS_TOKEN"


Visit [localhost:3000](http://localhost:3000)


## Run on [Heroku](https://heroku.com)

A Procfile is included for simple deployment to Heroku.

- Create a new app in Heroku.
- Push this repository to it.
- Set the Config Variables in the app settings:

        VSO_ACCOUNT_NAME
        VSO_PROJECT_NAME
        VSO_PERSONAL_ACCESS_TOKEN


## Development

Run tests:

    lein midje [:autotest]

#### TODO:

- reinstate error handling on front end (was removed from `refresh.js`) - VSO api sometimes goes down and it is useful to get some feedback in the browser
- fix the favicon (at the moment it takes its cue from builds only)

