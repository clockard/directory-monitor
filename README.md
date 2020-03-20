# Directory Monitor
[![CircleCI](https://circleci.com/gh/clockard/directory-monitor.svg?style=shield)](https://app.circleci.com/pipelines/github/clockard/directory-monitor)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=clockard_directory-monitor&metric=alert_status)](https://sonarcloud.io/dashboard?id=clockard_directory-monitor)
## Overview
Directory monitor does just what it says monitors a filesystem directory for file creates, updates, and deletes.
Key features are:
- Remembers between restarts where it was and process any files that were changed since it last ran
- Allows association of a forgien key with each file 
- Allows file name pattern matching
- Configurable retry period for files that fail to process
- Plugable file processors
